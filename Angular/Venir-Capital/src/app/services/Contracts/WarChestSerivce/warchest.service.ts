import { Injectable } from '@angular/core';
import { ConnectWalletService } from '../../Mana/ConnectPersonalWallet/connect-wallet.service';
import { warchestServiceContract } from './warchestServiceContract';
import { readContract, writeContract, getGasPrice, simulateContract, waitForTransactionReceipt } from '@wagmi/core'
import { BaseError, formatEther, parseGwei, parseEther } from 'viem';
import { AlertService } from '../../Alerts/alert.service';
import { numberToHex } from 'viem'
import { config } from '../config';
import { polygon } from '@wagmi/core/chains';
import { ganache } from '../ganache';
import { environment } from 'src/Environment/environment';

const chainID = environment.production ? 137 : 1337

@Injectable({
  providedIn: 'root'
})
export class WarchestService {
  web3:  any;

  constructor(
    private connectWalletService:ConnectWalletService,
    private alertService:AlertService
  ) { }

  async callGetUserBalance(_userID: string): Promise<number | undefined> {
    return new Promise<number | undefined>((resolve, ) => {
      readContract(config,{
        address: warchestServiceContract.contractAddress as `0x${string}`,
        abi: warchestServiceContract.abi,
        functionName: 'getCreatorsBalance',
        args: [_userID],
      }).then(data => {
        resolve(Number(formatEther(data as bigint)))
      })
    })
  }

  async callUserWithdraw(_userID: string, dollarAmount: number): Promise<string | undefined> {
    return new Promise<string | undefined>((resolve, reject) => {
      getGasPrice(config, {
        chainId: chainID, 
      }).then(gasPrice => {
        const networkGasPrice = gasPrice;
        let increasedGasPrice;
        if(networkGasPrice){
          increasedGasPrice = networkGasPrice + parseGwei('10')
        }
        else{
          increasedGasPrice = parseGwei('60')
        }
        const request = {
          address: warchestServiceContract.contractAddress as `0x${string}`,
          abi: warchestServiceContract.abi,
          functionName: 'userWithdraw',
          gasPrice: increasedGasPrice,
          args: [_userID, parseEther(dollarAmount.toString())],
        };
        simulateContract(config, request).then(() => {
          writeContract(config, request).then(transactionHash => {
            waitForTransactionReceipt(config, {
              hash: transactionHash,
            }).then(result => {
              resolve(result.blockHash) 
            }).catch(error => {
              if (error instanceof BaseError) {
                this.handleError(error);
              }
              reject(error);
            });
          }).catch(error => {
            if (error instanceof BaseError) {
              this.handleError(error);
            }
            reject(error);
          });
        }).catch(error => {
          if (error instanceof BaseError) {
            this.handleError(error);
          }
          reject(error);
        });
      }).catch(error => {
        if (error instanceof BaseError) {
          this.handleError(error);
        }
        reject(error);
      });
    });
  }

  private handleError(error: BaseError) {
    this.alertService.addAlert(`Error: ${error.message}`, "error");
  }
}
