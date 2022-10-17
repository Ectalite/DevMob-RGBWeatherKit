//
//  BlePeripheral.swift
//  Basic Chat MVC
//
//  Created by Trevor Beaton on 2/14/21.
//

import Foundation
import CoreBluetooth

class BlePeripheral {
    static var connectedPeripheral: CBPeripheral?
    static var connectedService: CBService?
    static var connectedPixelXChar: CBCharacteristic?
    static var connectedPixelYChar: CBCharacteristic?
    static var connectedColorChar: CBCharacteristic?
    static var connectedSendChar: CBCharacteristic?
}
