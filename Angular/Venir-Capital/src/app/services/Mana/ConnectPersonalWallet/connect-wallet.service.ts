import {Inject, Injectable} from '@angular/core';
import { environment } from '../../../../Environment/environment'
import { createWeb3Modal, defaultWagmiConfig, Web3Modal } from '@web3modal/wagmi'
import { watchAccount , disconnect, reconnect, getAccount, getBalance, watchConnections } from '@wagmi/core'
import { config } from '../../Contracts/config';
import { formatEther } from 'viem'
import { ganache } from '../../Contracts/ganache';
import { polygon } from '@wagmi/core/chains';

@Injectable({
  providedIn: 'root'
})
export class ConnectWalletService {
  private web3Modal: Web3Modal

  //Constructs web3 modal for the connection of a wallet
  constructor() {
    const projectId = environment.Wallet_Connect_Project_Id
    const metadata = {
      name: 'Sivantis',
      description: 'AppKit Example',
      url: 'https://sivantis.com', // origin must match your domain & subdomain.
      icons: ['https://avatars.githubusercontent.com/u/37784886']
    }
    const chain = environment.production ? polygon : ganache
    const chains = [chain] as const
    const modalConfig = defaultWagmiConfig({
      chains,
      projectId,
      metadata,
    })

    
    // 3. Create modal
    this.web3Modal = createWeb3Modal({
      wagmiConfig: modalConfig,
      projectId,
      allowUnsupportedChain: true
    })
  } 

  isConnected(): boolean {
    return getAccount(config).isConnected
  }

  getConnectedAccount(): string | undefined{
    return getAccount(config).address
  }
  
  //Opens the web3Modal, connects Sivantis with user's personal wallet, and returns their wallet address.
  async connectAccount(): Promise<void> {
    return new Promise<void>(async (resolve, reject) => {
      try {
        this.web3Modal.open()
        this.web3Modal.subscribeState(state => {
          if(!state.open){
            resolve()
          }
        })
      } catch (error) {
        reject(error); // Reject the promise if there's an error
      }
    });
  }

  async disconnectAccount(): Promise<void> {
    const connector  = getAccount(config).connector
    disconnect(config, {
      connector, 
    })
  }

  //Views and returns the balance of the user's connect wallet
  async accountManabalance(account: string): Promise<number> {
    return new Promise<number>((resolve, ) => {
      getBalance(config, {
        address: account as `0x${string}`,
        token: environment.Contract_Decentraland_Mana_Address as `0x${string}`,
        unit: 'ether' 
      }).then(balance => {
          if(balance){
            resolve(Number(formatEther(balance.value)));
          }
        })
    })
  }

  //Views and returns the balance of the user's connect wallet
  async accountMaticbalance(account: string): Promise<number> {
    return new Promise<number>((resolve, ) => {
      getBalance(config, {
        address: account as `0x${string}`,
        unit: 'ether' 
      }).then(balance => {
          if(balance){
            resolve(Number(formatEther(balance.value)));
          }
      })
    })
  }


}


