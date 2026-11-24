"use strict";

const OUTPUT_CAPACITY = 16 * 1024 * 1024;
const SETTINGS = {
  sections: ["scene", "objects", "text", "relations", "reasoning"],
  fields: ["bbox", "score", "description", "mask"],
  mask_format: "polygons"
};

let engineInstance = null;
let outputBuffer = 0;
let isModelLoaded = false;

async function initEngine() {
  if (engineInstance && isModelLoaded) return engineInstance;
  
  self.postMessage({ type: "progress", label: "Loading WebAssembly runtime..." });
  
  // Load cactus.js
  importScripts("cactus.js");
  
  // Fetch cactus.wasm
  const wasmResponse = await fetch("cactus.wasm");
  if (!wasmResponse.ok) throw new Error("Failed to load cactus.wasm");
  const wasmBinary = await wasmResponse.arrayBuffer();
  
  engineInstance = await createCactus({ wasmBinary });
  self.postMessage({ type: "progress", label: "Loading Mirage 22MB weights..." });
  
  // Fetch mirage.cact
  const modelResponse = await fetch("mirage.cact");
  if (!modelResponse.ok) throw new Error("Failed to load mirage.cact");
  const modelBinary = await modelResponse.arrayBuffer();
  
  const modelBytes = new Uint8Array(modelBinary);
  const ptr = engineInstance._malloc(modelBytes.length);
  if (!ptr) throw new Error("Could not allocate memory for Mirage model");
  
  engineInstance.HEAPU8.set(modelBytes, ptr);
  const loadCode = engineInstance._cactus_load(ptr, BigInt(modelBytes.length));
  if (loadCode < 0) {
    engineInstance._free(ptr);
    throw new Error("Failed to load Mirage weights (code: " + loadCode + ")");
  }
  
  outputBuffer = engineInstance._malloc(OUTPUT_CAPACITY);
  if (!outputBuffer) throw new Error("Could not allocate output buffer");
  
  isModelLoaded = true;
  self.postMessage({ type: "ready", label: "Mirage loaded and ready" });
  return engineInstance;
}

let loadingPromise = null;

async function describeFrame({ id, rgb, width, height }) {
  try {
    if (!loadingPromise) {
      loadingPromise = initEngine();
    }
    const engine = await loadingPromise;
    
    self.postMessage({ id, type: "running" });
    const startTime = performance.now();
    
    const inputPtr = engine._malloc(rgb.byteLength);
    if (!inputPtr) throw new Error("Could not allocate input buffer for frame");
    
    try {
      engine.HEAPU8.set(rgb, inputPtr);
      const code = engine.ccall(
        "cactus_describe",
        "number",
        ["number", "number", "number", "string", "number", "number", "number", "number"],
        [inputPtr, width, height, JSON.stringify(SETTINGS), 0, 0, outputBuffer, OUTPUT_CAPACITY]
      );
      
      if (code < 0) {
        const errPtr = engine.ccall("cactus_last_error", "number", [], []);
        throw new Error(engine.UTF8ToString(errPtr) || "Inference failed with code " + code);
      }
      
      const jsonString = engine.UTF8ToString(outputBuffer);
      const result = JSON.parse(jsonString);
      const elapsedMs = performance.now() - startTime;
      
      self.postMessage({
        id,
        type: "result",
        result,
        milliseconds: elapsedMs,
        settings: SETTINGS
      });
    } finally {
      engine._free(inputPtr);
    }
  } catch (err) {
    self.postMessage({
      id,
      type: "error",
      message: err instanceof Error ? err.message : String(err)
    });
  }
}

let queue = Promise.resolve();
self.onmessage = (event) => {
  const data = event.data;
  if (data.type === "init") {
    loadingPromise = initEngine().catch(err => {
      self.postMessage({ type: "error", message: err.message });
      loadingPromise = null;
    });
  } else {
    queue = queue.then(() => describeFrame(data));
  }
};
