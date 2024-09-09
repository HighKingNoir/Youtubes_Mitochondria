import { http, createConfig } from '@wagmi/core'
import { polygon } from '@wagmi/core/chains'
import { ganache } from './ganache'
import { coinbaseWallet, walletConnect } from '@wagmi/connectors'
import { environment } from 'src/Environment/environment'

const chain = environment.production ? polygon : ganache

export const config = createConfig({
  chains: [chain],
  connectors: [
    walletConnect({
      projectId: environment.Wallet_Connect_Project_Id,
    }),
  ],
  pollingInterval: 1_000,
  transports: {
    [ganache.id]: http(),
    [polygon.id]: http(),
  },
})

