/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare global {
  interface Window {
    REACT_APP_NAVIGATE: any;
  }
  
  const __BASE_PATH__: string;
}

export {};
