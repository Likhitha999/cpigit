import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

def Message processData(Message message) {
  def body = message.getBody(String.class)
  def jsonSlurper = new JsonSlurper()
  def object = jsonSlurper.parseText(body)

  // Checking if there is only one Record
  if (!isCollectionOrArray(object.companyCodeReplicationRequest.companyCode)) {
    // Converting record to an array
    object.companyCodeReplicationRequest.companyCode = [object.companyCodeReplicationRequest.companyCode].toArray()
  }

  // Set addressLine1 and addressLine2 
  object.companyCodeReplicationRequest.companyCode.each {
    record ->
      if (record.addressData != null && record.addressData.addressLines != null) {
        if (!isCollectionOrArray(record.addressData.addressLines)) {
          // Converting record to an array
          record.addressData.addressLines = [record.addressData.addressLines].toArray()
        }

        record.addressData.addressLine1 = record.addressData.addressLines[0];
        if (record.addressData.addressLines.size() == 2) {
          record.addressData.addressLine2 = record.addressData.addressLines[1];
        }
        // Remove addressLines from addressData
        record.addressData.remove('addressLines')
      }
  }
  
  // Set the total no. of records to exchange property 'total_records'
  message.setProperty('total_records', object.companyCodeReplicationRequest.companyCode.size())

  // Set DRF bulk message id and sender system id as properties
  message.setProperty('messageID', object.messageID)
  message.setProperty('senderID', object.senderID)

  def finalPayload = JsonOutput.toJson(object.companyCodeReplicationRequest.companyCode)
  
  // Unescape unicode characters
  def decodedString = new UnicodeUnescaper().translate(finalPayload)
  
  // Prepare Message Body
  message.setBody(decodedString)

  // Prepare Message Header
  message.setHeader('Content-Type', 'application/json')

  return message
}

// Returns true if object is an array
boolean isCollectionOrArray(object) {
  [Collection, Object[]].any {
    it.isAssignableFrom(object.getClass())
  }
}