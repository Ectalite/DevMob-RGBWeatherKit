//
//  CBUUIDs.swift
//  Basic Chat MVC
//
//  Created by Trevor Beaton on 2/3/21.
//

import Foundation
import CoreBluetooth

struct CBUUIDs{
    static let kBLEServiceSearch_UUID = "1800"
    static let kBLEService_UUID = "11223344-5566-7788-99AA-BBCCDDEEFFFF"
    static let kBLE_Characteristic_uuid_Name = "BADDCAFE-0000-0000-0000-000000000001"
    static let kBLE_Characteristic_uuid_PixelX = "BADDCAFE-0000-0000-0000-000000000002"
    static let kBLE_Characteristic_uuid_PixelY = "BADDCAFE-0000-0000-0000-000000000003"
    static let kBLE_Characteristic_uuid_Color = "BADDCAFE-0000-0000-0000-000000000004"
    static let kBLE_Characteristic_uuid_Send = "BADDCAFE-0000-0000-0000-000000000005"
    static let MaxCharacters = 20

    static let BLEServiceSearch_UUID = CBUUID(string: kBLEServiceSearch_UUID)
    static let BLEService_UUID = CBUUID(string: kBLEService_UUID)
    //(Property = ReadOnly)
    static let BLE_Characteristic_uuid_Name = CBUUID(string: kBLE_Characteristic_uuid_Name)
    //(Property = Read and Write without response)
    static let BLE_Characteristic_uuid_PixelX = CBUUID(string: kBLE_Characteristic_uuid_PixelX)
    static let BLE_Characteristic_uuid_PixelY = CBUUID(string: kBLE_Characteristic_uuid_PixelY)
    static let BLE_Characteristic_uuid_Color = CBUUID(string: kBLE_Characteristic_uuid_Color)
    static let BLE_Characteristic_uuid_Send = CBUUID(string: kBLE_Characteristic_uuid_Send)
}
