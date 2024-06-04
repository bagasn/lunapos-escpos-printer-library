// main index.js

import { NativeModules } from 'react-native';

const { LunaEscposPrinter, LunaBluetoothPrinter, LunaUsbPrinter } = NativeModules;

export { LunaBluetoothPrinter, LunaUsbPrinter };
export default LunaEscposPrinter;
