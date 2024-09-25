import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

def Message processData(Message message) {
    def body = message.getBody(String.class)
    def jsonSlurper = new JsonSlurper()
    def object = jsonSlurper.parseText(body)

    // Checking if there is only one Record
    if (!isCollectionOrArray(object.replicationMaterialMasterGeneralDataRequest)) {
      // Converting record to an array
      object.replicationMaterialMasterGeneralDataRequest = [object.replicationMaterialMasterGeneralDataRequest].toArray()
    }

    // Set DRF bulk message id and sender system id as properties
    message.setProperty('messageID', object.DRF_Id)
    message.setProperty('senderID', object.Sender_Id)

    // Extracting data without 'replicationMaterialMasterGeneralDataRequest' key
    def request = object.replicationMaterialMasterGeneralDataRequest.collect {
      item ->
        item.remove('replicationMaterialMasterGeneralDataRequest')
      return item
    }
    
    // Set the total no. of records to exchange property 'total_records'
    message.setProperty('total_records', request.size())

    // Set netWeight, grossWeight, volume, weightUnit_code and volumeUnit_code
    request.each {
      item ->
        if (item.quantityCharacteristic != null) {
          if (!isCollectionOrArray(item.quantityCharacteristic)) {
            //Converting record to an array
            item.quantityCharacteristic = [item.quantityCharacteristic].toArray()
          }

          item.quantityCharacteristic.each {
            record ->
              if (record.characteristicQuantityTypeCode == 'NET_WEIGHT') {
                item.netWeight = record.characteristicQuantity
                item.weightUnit_code = record.characteristicQuantityUnitCode
              }
            if (record.characteristicQuantityTypeCode == 'GROSS_WT') {
              item.grossWeight = record.characteristicQuantity
              item.weightUnit_code = record.characteristicQuantityUnitCode
            }
            if (record.characteristicQuantityTypeCode == 'VOLUME') {
              item.volume = record.characteristicQuantity
              item.volumeUnit_code = record.characteristicQuantityUnitCode
            }
          }

        }
        
      // Remove quantityCharacteristic from payload
      item.remove('quantityCharacteristic')

      if (item.texts != null) {
        if (!isCollectionOrArray(item.texts)) {
          // Converting record to an array
          item.texts = [item.texts].toArray()
        }

        // Set material description from texts if locale is english
        item.texts.each {
          text ->
            if (text.locale == 'en') {
              item.materialDescription = text.materialDescription
            }
        }
        
        if (item.materialDescription == null) {
          object.row[i].materialDescription = item.texts[0].materialDescription
        }
      }

      if (item.materialMasterPlantData != null) {
        if (!isCollectionOrArray(item.materialMasterPlantData)) {
          // Converting record to an array
          item.materialMasterPlantData = [item.materialMasterPlantData].toArray()
        }
      }

      if (item.valuationArea != null) {
        if (!isCollectionOrArray(item.valuationArea)) {
          // Converting record to an array
          item.valuationArea = [item.valuationArea].toArray()
        }
      }

      // Set standardPrice_price and movingAveragePrice_price based on valuationAreaID and inventoryValuationProcedureCode
      item.materialMasterPlantData.each {
        record ->
          item.valuationArea.each {
            entry ->
              if (entry.ledgerAccount != null) {
                if (!isCollectionOrArray(entry.ledgerAccount)) {
                  entry.ledgerAccount = [entry.ledgerAccount].toArray()
                }
                if (record.plant == entry.valuationAreaID) {
                  entry.ledgerAccount.each {
                    account ->
                      if (account.inventoryValuationProcedureCode != null) {
                          record.pricingIndicator = account.inventoryValuationProcedureCode
                        }
                      if(account.priceAmountValue != null && account.priceAmountValue != ''){
                         record.standardPrice_price = new BigDecimal(account.priceAmountValue)
                        }
                      }
                  }
                }
              }
            if (record.safetyStock != null && record.safetyStock != '') {
              record.safetyStock = record.safetyStock.contains('.') ? record.safetyStock.toDouble().toInteger() : record.safetyStock.toInteger()
            }
            if (record.maximumStockLevel != null && record.maximumStockLevel != '') {
              record.maximumStockLevel = record.maximumStockLevel.contains('.') ? record.maximumStockLevel.toDouble().toInteger() : record.maximumStockLevel.toInteger()
            }
            if (record.reorderPoint != null && record.reorderPoint != '') {
              record.reorderPoint = record.reorderPoint.contains('.') ? record.reorderPoint.toDouble().toInteger().toString() : record.reorderPoint
            }
          }
        // Remove valuationArea from payload
        item.remove('valuationArea')
      }

      def finalPayload = JsonOutput.toJson(request)
      
      // Unescape unicode characters
      def decodedString = new UnicodeUnescaper().translate(finalPayload)

      // Prepare Message Body
      message.setBody(decodedString)

      // Prepare Message Header
      message.setHeader('Content-Type', "application/json")

      return message
    }

    //Returns true if object is an array
    boolean isCollectionOrArray(object) {
      [Collection, Object[]].any {
        it.isAssignableFrom(object.getClass())
      }
    }