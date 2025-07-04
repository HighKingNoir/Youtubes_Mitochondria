import { http, createConfig, createStorage, cookieStorage } from '@wagmi/core'
import { polygon } from '@wagmi/core/chains'
import { ganache } from './ganache'
import { coinbaseWallet, walletConnect } from '@wagmi/connectors'
import { environment } from 'src/Environment/environment'
import { WagmiAdapter } from '@reown/appkit-adapter-wagmi'

const chain = environment.production ? polygon : ganache
const network = environment.production ? polygon : ganache
const projectId = environment.Wallet_Connect_Project_Id

export const wagmiAdapter = new WagmiAdapter({
  networks: [network],
  // ssr: true,
  projectId
})


export const config = wagmiAdapter.wagmiConfig

