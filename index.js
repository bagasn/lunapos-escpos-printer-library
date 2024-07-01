// main index.js

import { NativeModules } from 'react-native';

const { LunaEscposPrinter, LunaBluetoothPrinter, LunaUsbPrinter, BluetoothManagerModule } = NativeModules;

export { LunaBluetoothPrinter, LunaUsbPrinter, BluetoothManagerModule };
export default LunaEscposPrinter;
