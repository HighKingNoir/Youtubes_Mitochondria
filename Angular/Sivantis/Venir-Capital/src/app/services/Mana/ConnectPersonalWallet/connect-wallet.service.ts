import {Inject, Injectable} from '@angular/core';
import { environment } from '../../../../Environment/environment'
import { getAccount, getBalance } from '@wagmi/core'
import { config } from '../../Contracts/config';
import { formatEther } from 'viem'
import { ganache } from '../../Contracts/ganache';
import { connect, getConnectors } from '@wagmi/core';
import { AppKit, createAppKit } from '@reown/appkit/react'
import { polygon } from '@reown/appkit/networks'
import { wagmiAdapter } from '../../Contracts/config';

@Injectable({
  providedIn: 'root'
})
export class ConnectWalletService {
  private web3Modal: AppKit
  Logo = "assets/Logo.png"

  //Constructs web3 modal for the connection of a wallet
  constructor() {
    const projectId = environment.Wallet_Connect_Project_Id
    const metadata = {
      name: 'Sivantis',
      description: 'AppKit Example',
      url: environment.FrontEndURL.replace(/\/$/, ''), // origin must match your domain & subdomain.
      icons: [this.Logo]
    }
    const network = environment.production ? polygon : ganache

    this.web3Modal = createAppKit({
      adapters: [wagmiAdapter],
      networks: [network],
      metadata: metadata,
      projectId,
      features: {
        analytics: true,
        email: false,
        socials: ['google', 'x', 'github', 'discord'],
      }
     })

  } 

  isConnected(): boolean {
    return this.web3Modal.getAccount()?.status == "connected"
  }

  // connection.connector.getChainId is not a function

  getConnectedAccount(): string | undefined{
    return this.web3Modal.getAccount()?.address
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
    return new Promise<void>((resolve, ) => {
      this.web3Modal.disconnect("eip155").then(result => {
        localStorage.removeItem("@appkit/ens_cache")
        localStorage.removeItem("@appkit/native_balance_cache")
        resolve()
      })
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


