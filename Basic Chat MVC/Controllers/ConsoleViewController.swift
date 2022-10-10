//
//  ConsoleViewController.swift
//  Basic Chat
//
//  Created by Trevor Beaton on 2/6/21.
//

import UIKit
import CoreBluetooth

class ConsoleViewController: UIViewController {

    //Data
    var peripheralManager: CBPeripheralManager?
    var peripheral: CBPeripheral?

    @IBOutlet weak var peripheralLabel: UILabel!
    @IBOutlet weak var serviceLabel: UILabel!
    @IBOutlet weak var xPosLabel: UILabel!
    @IBOutlet weak var yPosLabel: UILabel!
    @IBOutlet weak var xPosValue: UILabel!
    @IBOutlet weak var yPosValue: UILabel!
    @IBOutlet weak var xPosSlider: UISlider!
    @IBOutlet weak var yPosSlider: UISlider!
    @IBOutlet weak var testLabel: UILabel!
    @IBOutlet weak var sendButton: UIButton!
    
    var testInt : Int = 0
    
    override func viewDidLoad()
    {
        super.viewDidLoad()

        keyboardNotifications()

        /*NotificationCenter.default.addObserver(self, selector: #selector(self.appendRxDataToTextView(notification:)), name: NSNotification.Name(rawValue: "Notify"), object: nil)*/
        //sendButton.addTarget(self, action: #selector(self.onClick(_:)), for: .touchDown)
        //sendButton.addTarget(self, action: #selector(self.onRelease(_:)), for: .touchUpInside)
        peripheralLabel.text = BlePeripheral.connectedPeripheral?.name
    
        if (BlePeripheral.connectedService != nil) {
          serviceLabel.text = "Number of Services: \(String((BlePeripheral.connectedPeripheral?.services!.count)!))"
        }
        else
        {
          print("Service was not found")
        }
    }
    
    @IBAction func xPosSliding(_ sender: Any)
    {
        xPosValue.text = "\(Int(xPosSlider.value))"
        xPosValue.sizeToFit()
        sendButton.tintColor = UIColor.gray
    }
    
    @IBAction func yPosSliding(_ sender: Any)
    {
        yPosValue.text = "\(Int(yPosSlider.value))"
        yPosValue.sizeToFit()
        sendButton.tintColor = UIColor.black
    }
    @IBAction func onClick(_ sender: Any)
    {
        print("Pressed")
        testInt+=1
        testLabel.text = "Test Var: \(testInt)"
        testLabel.sizeToFit()
        sendBT(xPos: Int(xPosSlider.value), yPos: Int(yPosSlider.value))
    }
    
    func keyboardNotifications()
    {
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillChange(notification:)), name: UIResponder.keyboardWillShowNotification, object: nil)

        NotificationCenter.default.addObserver(self, selector: #selector(keyboardDidHide(notification:)), name: UIResponder.keyboardDidHideNotification, object: nil)

        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillChange(notification:)), name: UIResponder.keyboardWillChangeFrameNotification, object: nil)
    }

    deinit
    {
        NotificationCenter.default.removeObserver(self, name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: UIResponder.keyboardDidHideNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: UIResponder.keyboardWillChangeFrameNotification, object: nil)
    }

  // MARK:- Keyboard
    @objc func keyboardWillChange(notification: Notification) {
        if let keyboardSize = (notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue
        {

          let keyboardHeight = keyboardSize.height
          print(keyboardHeight)
          view.frame.origin.y = (-keyboardHeight + 50)
        }
    }

    @objc func keyboardDidHide(notification: Notification)
    {
        view.frame.origin.y = 0
    }

    @objc func disconnectPeripheral()
    {
        print("Disconnect for peripheral.")
    }

    // Write functions
    func sendBT(xPos: Int, yPos: Int){
        var pxPos = xPos
        var pyPos = yPos
        var send = 1
        let xPosData = Data(bytes: &pxPos, count: 1)
        let yPosData = Data(bytes: &pyPos, count: 1)
        let colorString = ("test" as NSString).data(using: String.Encoding.ascii.rawValue)
        let sendData = Data(bytes: &send, count: 1)
        //change the "data" to valueString
        let blePeripheral = BlePeripheral.connectedPeripheral
        blePeripheral!.writeValue(xPosData, for: BlePeripheral.connectedPixelXChar!, type: CBCharacteristicWriteType.withoutResponse)
        blePeripheral!.writeValue(yPosData, for: BlePeripheral.connectedPixelYChar!, type: CBCharacteristicWriteType.withoutResponse)
        blePeripheral!.writeValue(colorString!, for: BlePeripheral.connectedColorChar!, type: CBCharacteristicWriteType.withoutResponse)
        blePeripheral!.writeValue(sendData, for: BlePeripheral.connectedSendChar!, type: CBCharacteristicWriteType.withoutResponse)
    }
}

extension ConsoleViewController: CBPeripheralManagerDelegate {

  func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
    switch peripheral.state {
    case .poweredOn:
        print("Peripheral Is Powered On.")
    case .unsupported:
        print("Peripheral Is Unsupported.")
    case .unauthorized:
    print("Peripheral Is Unauthorized.")
    case .unknown:
        print("Peripheral Unknown")
    case .resetting:
        print("Peripheral Resetting")
    case .poweredOff:
      print("Peripheral Is Powered Off.")
    @unknown default:
      print("Error")
    }
  }

  //Check when someone subscribe to our characteristic, start sending the data
  func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didSubscribeTo characteristic: CBCharacteristic) {
      print("Device subscribe to characteristic")
  }

}


