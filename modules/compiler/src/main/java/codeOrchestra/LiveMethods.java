package codeOrchestra;

/**
 * @author Alexander Eliseyev
 */
public enum LiveMethods {
  
  ALL("all"),
  ANNOTATED("annotated");
  
  private String preferenceValue;

  LiveMethods(String preferenceValue) {
    this.preferenceValue = preferenceValue;
  }
  
  public static LiveMethods parseValue(String value) {
    for (LiveMethods liveMethods : LiveMethods.values()) {
      if (liveMethods.preferenceValue.equals(value)) {
        return liveMethods;
      }
    }
    
    return null;
  }
  
}
