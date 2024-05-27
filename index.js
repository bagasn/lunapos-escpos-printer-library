// main index.js

import { NativeModules } from 'react-native';

const { LunaEscposPrinter, LunaBluetoothPrinter } = NativeModules;

export { LunaBluetoothPrinter };
export default LunaEscposPrinter;
