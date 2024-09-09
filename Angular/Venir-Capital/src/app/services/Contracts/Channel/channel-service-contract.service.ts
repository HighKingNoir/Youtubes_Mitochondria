import { Injectable } from '@angular/core';
import { channelServiceContract } from './channelServiceContract';
import { readContract , writeContract , getGasPrice, waitForTransactionReceipt, simulateContract } from '@wagmi/core'
import { BaseError, formatEther, parseEther, parseGwei } from 'viem';
import { AlertService } from '../../Alerts/alert.service';
import { config } from '../config';
import { polygon } from '@wagmi/core/chains';
import { ganache } from '../ganache';
import { environment } from 'src/Environment/environment';

const chainID = environment.production ? 137 : 1337

@Injectable({
  providedIn: 'root'
})
export class ChannelServiceContract {
  
  constructor(
    private alertService: AlertService
  ) { }

  async callfundChannel(_channelName: string, _mana: string): Promise<string | undefined> {
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
          address: channelServiceContract.contractAddress as `0x${string}`,
          abi: channelServiceContract.abi,
          functionName: 'fundChannel',
          args: [_channelName, parseEther(_mana)],
          gasPrice: increasedGasPrice,
        };
        simulateContract(config, request).then(()=> {
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

  async callGetChannelBalance(_channelName: string): Promise<number | undefined> {
    return new Promise<number | undefined>((resolve, ) => {
      readContract(config,{
        address: channelServiceContract.contractAddress as `0x${string}`,
        abi: channelServiceContract.abi,
        functionName: 'getChannelBalance',
        args: [_channelName],
      }).then(data => {
        resolve(Number(formatEther(data as bigint)))
      })
    })
  }
    

  private handleError(error: BaseError) {
    this.alertService.addAlert(`Error: ${error.message}`, "error");
  }
}
