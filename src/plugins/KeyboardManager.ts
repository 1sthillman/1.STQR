import { registerPlugin } from '@capacitor/core';

export interface KeyboardStatus {
  enabled: boolean;
  selected: boolean;
  keyboardId: string;
}

export interface KeyboardManagerPlugin {
  openInputMethodSettings(): Promise<void>;
  showKeyboardPicker(): Promise<void>;
  getStatus(): Promise<KeyboardStatus>;
}

export const KeyboardManager = registerPlugin<KeyboardManagerPlugin>('KeyboardManager');











