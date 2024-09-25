import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;

def Message processData(Message message) {
  def body = message.getBody(String.class)
  def jsonSlurper = new JsonSlurper()
  def object = jsonSlurper.parseText(body)

  int success = 0
  int failure = 0
  def errors = []
  int total = message.getProperty('total_records')

  object.each {
    item ->
      try {
        if (item.Status == ('Error')) {
          item.remove('Status')
          item.remove('RecordNo')
          errors.push(item)
        } else {
          success = success + 1
        }
      } catch (Exception e) {
        errors.push(item)
      }
  }

  if(success == 0){
      failure = total
  } else {
      failure = total - success
  }
  
  def errorJson = [
    'Total_Records': total,
    'Success': success,
    'Failure': failure,
    'Errors': errors
  ]

  def response = JsonOutput.toJson(errorJson)

  def messageLog = messageLogFactory.getMessageLog(message)
  messageLog.addAttachmentAsString("Response", response, "text/plain")

  message.setBody(response)

  if (errors.size() > 0) {
    throw new CustomRPMException(errors.toString())
  }

  return message
}

class CustomRPMException extends Exception {
  CustomRPMException(String message) {
    super(message)
  }
}