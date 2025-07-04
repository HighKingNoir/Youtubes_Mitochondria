import { Injectable } from '@angular/core';
import { createPublicClient, createWalletClient, custom, getContract, http, parseSignature } from 'viem';
import { mainnet, polygon } from 'viem/chains';
import { environment } from 'src/Environment/environment';
import { gaslessFunctionCallContract } from './gaslessFunctionCallContract';
import { ConnectWalletService } from '../../Mana/ConnectPersonalWallet/connect-wallet.service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getPublicClient } from '@wagmi/core';
import { ganache } from '../ganache';
import { signTypedData } from '@wagmi/core'
import { config } from '../config';

type SupportedChainID = 137 | 1337;
const chainID: SupportedChainID = environment.production ? 137 : 1337;
const chain = environment.production ? polygon : ganache

@Injectable({
  providedIn: 'root'
})
export class GaslessFunctionCallService {
  gaslessFunctionCallAddress: string = environment.Contract_Gasless_Function_Call_Address;
  private decentralandManaAddress: string = environment.Contract_Decentraland_Mana_Address
  
  private SivantisURL:string = environment.BackendURL + 'Gasless';

  constructor(
    private connectWalletService:ConnectWalletService,
    private httpClient: HttpClient
  ) {
    
   }


    fundChannel(gaslessFunctionCallPayload: GaslessFunctionCallPayload): Observable<any>{
       return this.httpClient.post(this.SivantisURL + '/Fund', gaslessFunctionCallPayload, {responseType: 'text'});
    }

    placeBid(gaslessFunctionCallPayload: GaslessFunctionCallPayload): Observable<any>{
       return this.httpClient.post(this.SivantisURL + '/Place', gaslessFunctionCallPayload, {responseType: 'text'});
    }

    raiseBid(gaslessFunctionCallPayload: GaslessFunctionCallPayload): Observable<any>{
       return this.httpClient.post(this.SivantisURL + '/Raise', gaslessFunctionCallPayload, {responseType: 'text'});
    }

    cancelBid(gaslessFunctionCallPayload: GaslessFunctionCallPayload): Observable<any>{
       return this.httpClient.post(this.SivantisURL + '/Cancel', gaslessFunctionCallPayload, {responseType: 'text'});
    }

  async getPermitSignature({
    sender,
    amount,
    fee,
    deadline,
  }: {
    sender: string;
    amount: bigint;
    fee: bigint;
    deadline: BigInt;
  }) {

    const nonce = await publicClient.getTransactionCount({  
      address: sender as `0x${string}`,
    })

    const domainName = "Decentraland (MANA)"
    const domainVersion = "1" 
    const chainId = chainID 
    const contractAddress = this.decentralandManaAddress as `0x${string}`

    const domain = {
      name: domainName,
      version: domainVersion,
      verifyingContract: contractAddress,
      chainId
    }

    const Permit = [
        { name: "owner", type: "address" },
        { name: "spender", type: "address" },
        { name: "value", type: "uint256" },
        { name: "nonce", type: "uint256" },
        { name: "deadline", type: "uint256" },
      ]

      const value = amount + fee
    
      const permit = { 
        owner: sender, 
        spender: this.gaslessFunctionCallAddress, 
        value, 
        nonce,
        deadline }
      

    const walletClient = createWalletClient({
      chain: chain,
      transport: custom(window.ethereum!),
      account: sender as `0x${string}`
    })

    const signature = await walletClient.signTypedData({
      types: {
              // EIP712Domain: domainType,
              Permit: Permit
      },
      domain: domain,
      primaryType: "Permit",
      message: permit,
      account: sender as `0x${string}`
    });

    return signature

  }

}
export interface GaslessFunctionCallPayload{
   signature: String;
   sender: String;
   contentID: String | null;
   channelName: String | null;
   userID: String | null;
   amount: bigint;
   fee: bigint;
   deadline: BigInt;
}

export const publicClient = createPublicClient({
  chain: chain,
  transport: http(),
})