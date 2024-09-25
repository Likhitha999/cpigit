import com.sap.gateway.ip.core.customdev.util.Message;

def Message processData(Message message) {
  String errorDetails = "{\"Status\":\"Error\",\"Details\": \"" 
  String UnknownError = "Unknown Error, please contact your system administrator."
  def messageLog = messageLogFactory.getMessageLog(message)
  
  def ex = message.getProperty("CamelExceptionCaught")
  if (ex != null) {
    if(ex.getClass().getCanonicalName().equals("org.apache.camel.component.ahc.AhcOperationFailedException")){
        errorDetails = errorDetails + ex.getMessage() + "\"}"
        messageLog.addAttachmentAsString("Response", errorDetails, "text/plain")
        messageLog.setStringProperty('Exception', ex.getMessage())
    } else if (ex.getClass().getCanonicalName().equals("javax.script.ScriptException")) {
      // Check whether the ScriptException is caused by CustomRPMException
      if (getRootCause(ex).toString().contains("CustomRPMException")) {
        errorDetails = errorDetails + getRootCause(ex).getCause().getMessage() + "\"}"
      } else {
        errorDetails = errorDetails + UnknownError + "\"}"
        messageLog.addAttachmentAsString("Response", errorDetails, "text/plain")
        messageLog.setStringProperty('Exception', ex.getMessage())
      }
    } else {
      errorDetails = errorDetails + UnknownError + "\"}"
      messageLog.addAttachmentAsString("Response", errorDetails, "text/plain")
      messageLog.setStringProperty('Exception', ex.getMessage())
    }
  }

  message.setBody(errorDetails)

  return message
}

def getRootCause(Throwable throwable) {
  if (throwable.getCause() != null) {
    return throwable.getCause()
  }
}