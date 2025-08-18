// main index.js

import { NativeModules } from 'react-native';

const { LunaEscposPrinter, LunaBluetoothPrinter, LunaUsbPrinter, LunaNetworkPrinter, LunaBluetoothManager } = NativeModules;

export { LunaBluetoothPrinter, LunaUsbPrinter, LunaNetworkPrinter, LunaBluetoothManager };
export default LunaEscposPrinter;
