// main index.js

import { NativeModules } from 'react-native';

const { LunaEscposPrinter, LunaBluetoothPrinter, LunaUsbPrinter, LunaBluetoothManager } = NativeModules;

export { LunaBluetoothPrinter, LunaUsbPrinter, LunaBluetoothManager };
export default LunaEscposPrinter;
