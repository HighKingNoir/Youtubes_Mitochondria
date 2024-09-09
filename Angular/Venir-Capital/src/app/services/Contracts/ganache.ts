import { defineChain } from "viem";

export const ganache = defineChain({
  id: 1337, // Ganache default chain ID
  name: 'Ganache',
  nativeCurrency: { name: 'Ether', symbol: 'ETH', decimals: 18 },
  rpcUrls: {
    default: { http: ['http://127.0.0.1:8545'] }, // Default Ganache RPC URL
  },
  blockExplorers: {
    default: { name: 'Ganache Explorer', url: 'http://127.0.0.1:8545' }, // You can customize this URL if you have a local block explorer
  },
  contracts: {
    
  },
})
