/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package flex2.compiler.i18n;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import flash.util.StringJoiner;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.CompilerException;
import flex2.compiler.CompilerSwcContext;
import flex2.compiler.ResourceBundlePath;
import flex2.compiler.Source;
import flex2.compiler.SourcePath;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.tools.CommandLineConfiguration;
import flex2.tools.oem.internal.ApplicationCompilerConfiguration;

/**
 * Helper class used by I18nCompiler and other parts of the compiler
 * to handle I18N related code generation and translation.
 */
public class I18nUtils
{
	public static String CLASS_SUFFIX = "_properties";
	
	public static String COMPILED_RESOURCE_BUNDLE_INFO = "_CompiledResourceBundleInfo";

	public static String codegenCompiledResourceBundleInfo(String[] locales, SortedSet<String> bundleNames)
	{
		String packageName = "";
		String className = COMPILED_RESOURCE_BUNDLE_INFO;
				
		String lineSep = System.getProperty("line.separator");
		
		String[] codePieces = new String[]
		{
			"package ", packageName, lineSep,
			"{", lineSep, lineSep,
			"[ExcludeClass]", lineSep, lineSep,
			"public class " , className, lineSep,
			"{", lineSep,
	        "    public static function get compiledLocales():Array /* of String */", lineSep,
	        "    {", lineSep,
	        "        return ", codegenCompiledLocales(locales), ";", lineSep,
			"    }", lineSep, lineSep,
	        "    public static function get compiledResourceBundleNames():Array /* of String */", lineSep,
	        "    {", lineSep,
	        "        return ", codegenCompiledResourceBundleNames(bundleNames), ";", lineSep,
			"    }", lineSep,
			"}", lineSep, lineSep,
			"}", lineSep
		};
		return StringJoiner.join(codePieces, null);
	}

    /**
     * Returns a string like
     *   [ "en_US", "ja_JP" ]
     */
    private static String codegenCompiledLocales(String[] locales)
    {
    	StringJoiner.ItemStringer itemStringer = new StringJoiner.ItemQuoter();
        return "[ " + StringJoiner.join(locales, ", ", itemStringer) + " ]";
    }

    /**
     * Returns a string like
     *   [ "core", "controls", "MyApp" ]
     */
    private static String codegenCompiledResourceBundleNames(SortedSet<String> bundleNames)
    {
    	if (bundleNames == null)
    		return "[]";
    	
    	StringJoiner.ItemStringer itemStringer = new StringJoiner.ItemQuoter();
        return "[ " + StringJoiner.join(bundleNames, ", ", itemStringer) + " ]";
	}

	/**
	 * Generates the target file for compiling a resource module SWF.
	 * This is a temp file, named GeneratedResourceModuleN.as
	 * (where N is an integer chosen by Java) in a temp directory
	 * which contains nothing but these resource modules files.
	 * The autogenerated code for a resource module has the following form:
	 *
	 * package
	 * {
	 * 
	 * import flash.utils.getDefinitionByName;
	 * import mx.modules.ModuleBase;
	 * import mx.resources.IResourceModule;
	 * import mx.resources.ResourceBundle;
	 *
	 * [ExcludeClass]
	 * 
	 * [ResourceBundle("formatters")]
	 * [ResourceBundle("validators")]
	 * 
	 * public class GeneratedResourceModule123 extends ModuleBase
	 *     implements IResourceModule
	 * {
	 *     private static var resourceBundleClassNames:Array =
	 *     [
	 *         "formatters_properties",
	 *         "validators_properties"
	 *     ];
	 * 	
	 *     public function ResourceModule123()
	 *     {
	 *         super();
	 *     }
	 * 
	 *     private var _resourceBundles:Array;
	 * 
	 *     public function get resourceBundles():Array
	 *     {
	 *         if (!_resourceBundles)
	 *         {
	 *             _resourceBundles = [];
	 *             var n:int = resourceBundleClassNames.length;
	 *             for (var i:int = 0; i < n; i++)
	 *             {
	 *                 var resourceBundleClass:Class =
	 *                     Class(getDefinitionByName(resourceBundleClassNames[i]));
	 *                 var resourceBundle:ResourceBundle = new resourceBundleClass();
	 *                 _resourceBundles.push(resourceBundle);
	 *             }
	 *         }
	 * 
	 *         return _resourceBundles;
	 *     }
	 * }
	 * 
	 * }
	 */
    
    public static File getGeneratedResourceModule(ApplicationCompilerConfiguration configuration)
    {
        String[] locales = configuration.getCompilerConfiguration().getLocales();
        List bundleNames = configuration.getIncludeResourceBundles();
        
        return getGeneratedResourceModule(locales, bundleNames);
    }
    
    public static File getGeneratedResourceModule(CommandLineConfiguration configuration)
    {
        String[] locales = configuration.getCompilerConfiguration().getLocales();
        List bundleNames = configuration.getIncludeResourceBundles();
        
        return getGeneratedResourceModule(locales, bundleNames);
    }
    
    /**
     * Generate a target file for a resource module.
     * 
     * @param locales that we're compiling resource bundles for, specified via 
     * mxmlc's compiler.locale option
     * @param bundleNames of the resource bundles to be included in the resource
     * module, specified by mxmlc's include-resource-bundles option
     * @return
     */
    public static File getGeneratedResourceModule(String[] locales, List bundleNames)
    {
		// Create a directory for resource modules
		// (Adobe/Flex/GeneratedResourceModules inside of Java's tempdir)
		// if it doesn't already exist.
		String tempDirPath = System.getProperty("java.io.tmpdir");
        File tempDir = new File(tempDirPath, "Adobe");
        tempDir = new File(tempDir, "Flex");
		tempDir = new File(tempDir, "GeneratedResourceModules");
        tempDir.mkdirs();
		
		// Create an auto-deleting temp file for the autogenerated code
		// of the resource module class.
		File resourceModuleFile = null;
        try
		{
			resourceModuleFile = File.createTempFile("GeneratedResourceModule", ".as", tempDir);
		}
		catch (Exception e)
		{
			return null;
		}
        resourceModuleFile.deleteOnExit();
		
        return codegenResourceModule(resourceModuleFile, locales, bundleNames);
    }

    /**
     * Generate the AS resource module for the given locales and bundle names.
     * 
     * @param resourceModuleFile - name of the resource module file to create or rewrite
     * @param locales
     * @param bundleNames
     * @return
     */
    private static File codegenResourceModule(File resourceModuleFile, String[] locales, List bundleNames)
    {        
		// The class name of the resource module must match the file name
		// without the extension.
		String fileName = resourceModuleFile.getName();
		String className = fileName.substring(0, fileName.length() - 3);

		StandardDefs standardDefs = ThreadLocalToolkit.getStandardDefs();

		// Generate the code.
		String lineSep = System.getProperty("line.separator");
		String[] codePieces = new String[]
		{
			"package ", lineSep,
			"{", lineSep,
			lineSep,
			"import flash.utils.getDefinitionByName", lineSep,
			"import ", standardDefs.getModulesPackage(), ".ModuleBase;", lineSep,
			"import ", standardDefs.getResourcesPackage(), ".IResourceModule;", lineSep,
			"import ", standardDefs.getResourcesPackage(), ".ResourceBundle;", lineSep,
			lineSep,
			"[ExcludeClass]", lineSep, lineSep,
			codegenResourceBundleMetadata(bundleNames), lineSep,
			"public class ", className, " extends ModuleBase", lineSep,
			"    implements IResourceModule", lineSep,
			"{", lineSep,
			"    private static var resourceBundleClassNames:Array /* of String */ =", lineSep,
			"    [", lineSep,
			codegenResourceBundleClassNames(locales, bundleNames), lineSep,
	        "    ];", lineSep, lineSep,
			"    public function ", className, "()", lineSep,
	        "    {", lineSep,
			"        super();", lineSep,
			"    }", lineSep, lineSep,
			"    private var _resourceBundles:Array /* of ResourceBundle */;", lineSep, lineSep,
			"    public function get resourceBundles():Array /* of ResourceBundle */", lineSep,
			"    {", lineSep,
			"        if (!_resourceBundles)", lineSep,
			"        {", lineSep,
			"            _resourceBundles = [];", lineSep,
			"            var n:int = resourceBundleClassNames.length;", lineSep,
			"            for (var i:int = 0; i < n; i++)", lineSep,
			"            {", lineSep,
			"                var resourceBundleClass:Class =", lineSep,
			"                    Class(getDefinitionByName(resourceBundleClassNames[i]));", lineSep,
			"                var resourceBundle:ResourceBundle = new resourceBundleClass();", lineSep,
			"                _resourceBundles.push(resourceBundle);", lineSep,
			"            }", lineSep,
			"        }", lineSep, lineSep,
			"        return _resourceBundles;", lineSep,
			"    }", lineSep,
			"}", lineSep, lineSep,
			"}", lineSep
		};
		String code = StringJoiner.join(codePieces, null);
		
		// Write the code into the temp file.
		try
		{
	        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resourceModuleFile), "UTF8"));
	        bufferedWriter.write(code);
	        bufferedWriter.close();
		}
		catch (Exception e)
		{
		}
		      
		return resourceModuleFile;
	}
	
    /**
     * Regenerate a target file for a resource module.  Rewrite the file because
     * the resource bundles or locales have changed.
     *  
     * @param configuration for target file, locales and bundle names
     * @return regenerated resource module target file
     */
    public static File regenerateResourceModule(ApplicationCompilerConfiguration configuration)
    {        
        File resourceModuleFile = new File(configuration.getTargetFile());
        String[] locales = configuration.getCompilerConfiguration().getLocales();
        List bundleNames = configuration.getIncludeResourceBundles();
             
        return codegenResourceModule(resourceModuleFile, locales, bundleNames);
    }

	/*
	 * Returns a string containing of list of resource bundle metadata, such as
	 *   [ResourceBundle("core")]
	 *   [ResourceBundle("MyResources")]
	 */
	private static String codegenResourceBundleMetadata(List<String> bundleNames)
	{
		String lineSep = System.getProperty("line.separator");
		StringJoiner.ItemStringer itemStringer = new StringJoiner.ItemStringer()
    	{
    		public String itemToString(Object obj)
    		{
    			return "[ResourceBundle(\"" + (String)obj + "\")]";
    		}
    	};
    	return StringJoiner.join(bundleNames, lineSep, itemStringer) + lineSep;
	}
	
	/*
	 * Returns a string containing a list of resource bundle class names, such as
	 *   "en_US$core_properties",
	 *   "en_US$MyResources_properties",
	 *   "ja_JP$core_properties,
	 *   "ja_JP$MyResources_properties"
	 */
	private static String codegenResourceBundleClassNames(String[] locales, List<String> bundleNames)
	{
		String lineSep = System.getProperty("line.separator");
		
		// Loop over both the locales and the bundle names,
		// to build a list of class names.
		ArrayList<String> classNames = new ArrayList<String>();
		for (String locale : locales) {
			for (String bundleName : bundleNames) {
				String className = I18nUtils.getClassName(locale, bundleName);
				classNames.add(className);
			}
		}
    	
		// Output the list as comma-separated quoted strings.
		StringJoiner.ItemStringer itemStringer = new StringJoiner.ItemQuoter();
		return "        " + StringJoiner.join(classNames, "," + lineSep + "        ", itemStringer);
	}
	
	/*
	 * Returns the class name for the autogenerated resource bundle
	 * class for a specified locale and a bundleName.
	 * These class names, such as "en_US$core_properties",
	 * are constructed in such a way that the locale
	 * and bundle name can be extracted from them
	 * by the following two methods.
	 * 
	 * Note: The framework class mx.managers.SystemManager
	 * has similar logic which must be kept in sync with this.
	 */
	public static String getClassName(String locale, String bundleName)
	{
		return locale + "$" + bundleName + CLASS_SUFFIX;
	}

	public static Source getResourceBundleSource(String packageName, String className, ResourceBundlePath bundlePath,
	                                             SourcePath sourcePath, CompilerSwcContext swcContext)
			throws CompilerException
	{
		// look for className.properties
		Source s = bundlePath.findSource(packageName, className);

		if (s == null)
		{
			// continue our search for a .properties file by looking in the SWCs for className_properties
			s = swcContext.getSource(packageName, className + CLASS_SUFFIX);

			if (s == null)
			{
				// look for className.as
				s = sourcePath.findSource(packageName, className);

				// look in the SWCs for className
				if (s == null)
				{
					s = swcContext.getSource(packageName, className);
				}
			}
		}
		return s;
	}

	public static TranslationFormat getTranslationFormat(CompilerConfiguration config)
	{
		TranslationFormat format = null;
		try
		{
			Class formatCls = Class.forName(config.getTranslationFormat(), true,
					Thread.currentThread().getContextClassLoader());
			format = (TranslationFormat) formatCls.newInstance();
		}
		catch (Exception e)
		{
			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);
			ThreadLocalToolkit.logError(stringWriter.toString());
		}
		return format;
	}
	
	/*
	 * Extracts a locale such as "en_US" from a resource bundle
	 * class name such as "en_US$core_properties",
	 */
	public static String localeFromClassName(String className)
	{
		return className.substring(0, className.indexOf("$"));
	}
	
	/*
	 * Extracts a bundle name such as "core" from a resource bundle
	 * class name such as "en_US$core_properties",
	 */
	public static String bundleNameFromClassName(String className)
	{
		return className.substring(className.indexOf("$") + 1, className.length() - CLASS_SUFFIX.length());
	}
}
