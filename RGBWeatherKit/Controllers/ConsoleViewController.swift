//
//  ConsoleViewController.swift
//  Basic Chat
//
//  Created by Trevor Beaton on 2/6/21.
//

import UIKit
import CoreBluetooth

@IBDesignable class MyButton: UIButton
{
    override func layoutSubviews() {
        super.layoutSubviews()

        updateCornerRadius()
    }

    @IBInspectable var rounded: Bool = false {
        didSet {
            updateCornerRadius()
        }
    }

    func updateCornerRadius() {
        layer.cornerRadius = rounded ? frame.size.height / 2 : 0
    }
}

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
    @IBOutlet weak var sendButton: UIButton!
    @IBOutlet weak var colorView: UIStackView!
    @IBOutlet weak var colorLabel: UILabel!
    
    var testInt : Int = 0
    let colorWell: UIColorWell =
        {
            let colorWell = UIColorWell()
            colorWell.supportsAlpha = true
            colorWell.selectedColor = .white
            colorWell.title = "Pixel Color"
            return colorWell
        }()
    
    //Test function for color Weel
    /*@objc private func changeColor()
    {
        var rColor: CGFloat = 0
        var gColor: CGFloat = 0
        var bColor: CGFloat = 0
        var aColor: CGFloat = 0
        
        colorWell.selectedColor?.getRed(&rColor, green: &gColor, blue: &bColor, alpha: &aColor)
        
        rLabel.text = String(format:"%02X",Int8(truncatingIfNeeded: Int(rColor*255)))
        gLabel.text = String(format:"%02X",Int8(truncatingIfNeeded: Int(gColor*255)))
        bLabel.text = String(format:"%02X",Int8(truncatingIfNeeded: Int(bColor*255)))
        aLabel.text = String(format:"%02X",Int8(truncatingIfNeeded: Int(aColor*255)))
    }*/
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        colorView.addSubview(colorWell)
        colorWell.frame = CGRect(x:150, y: view.safeAreaInsets.top, width: view.frame.size.width-40, height:350)
        
        //colorWell.addTarget(self, action: #selector(changeColor), for: .valueChanged)
        
        xPosValue.text = "\(Int(xPosSlider.value))"
        xPosValue.sizeToFit()
        yPosValue.text = "\(Int(yPosSlider.value))"
        yPosValue.sizeToFit()
        keyboardNotifications()

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
    }
    
    @IBAction func yPosSliding(_ sender: Any)
    {
        yPosValue.text = "\(Int(yPosSlider.value))"
        yPosValue.sizeToFit()
    }
    @IBAction func onClick(_ sender: Any)
    {
        print("Pressed")
        testInt+=1
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
        var rColor: CGFloat = 0
        var gColor: CGFloat = 0
        var bColor: CGFloat = 0
        var aColor: CGFloat = 0
        var Colors: [Int8] = [0,0,0]
        
        colorWell.selectedColor?.getRed(&rColor, green: &gColor, blue: &bColor, alpha: &aColor)
        
        Colors[0] = Int8(truncatingIfNeeded: Int(rColor*255))
        Colors[1] = Int8(truncatingIfNeeded: Int(gColor*255))
        Colors[2] = Int8(truncatingIfNeeded: Int(bColor*255))
        
        let xPosData = Data(bytes: &pxPos, count: 1)
        let yPosData = Data(bytes: &pyPos, count: 1)
        let colorData = Data(bytes: &Colors, count: 3)
        
        //let colorData = Data(bytes: &color, count: 3)
        let sendData = Data(bytes: &send, count: 1)
        //change the "data" to valueString
        let blePeripheral = BlePeripheral.connectedPeripheral
        blePeripheral!.writeValue(xPosData, for: BlePeripheral.connectedPixelXChar!, type: CBCharacteristicWriteType.withoutResponse)
        blePeripheral!.writeValue(yPosData, for: BlePeripheral.connectedPixelYChar!, type: CBCharacteristicWriteType.withoutResponse)
        blePeripheral!.writeValue(colorData, for: BlePeripheral.connectedColorChar!, type: CBCharacteristicWriteType.withoutResponse)
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

extension UIColor {
    var rgba: (red: CGFloat, green: CGFloat, blue: CGFloat, alpha: CGFloat) {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        if getRed(&r, green: &g, blue: &b, alpha: &a) {
            return (r,g,b,a)
        }
        return (0, 0, 0, 0)
    }
}
