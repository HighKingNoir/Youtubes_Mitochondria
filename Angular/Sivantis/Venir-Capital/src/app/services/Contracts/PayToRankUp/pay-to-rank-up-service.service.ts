import { Injectable } from '@angular/core';
import { environment } from 'src/Environment/environment';
import { writeContract, simulateContract, waitForTransactionReceipt, getGasPrice, getAccount, reconnect, readContract } from '@wagmi/core'
import { BaseError, parseEther, parseGwei } from 'viem';
import { AlertService } from '../../Alerts/alert.service';
import { config } from '../config';
import { payToRankUpContract } from './payToRankUpContract';

type SupportedChainID = 137 | 1337;
const chainID: SupportedChainID = environment.production ? 137 : 1337;

@Injectable({
  providedIn: 'root'
})
export class PayToRankUpServiceService {

  constructor(
    private alertService: AlertService,
  ) { }

  async callArchonPass(_userID: string): Promise<string | undefined> {
      const account = getAccount(config);
      if(account.status != "connected"){
        await reconnect(config)
      }
      return new Promise<string | undefined>((resolve, reject) => {
        getGasPrice(config, {
          chainId: chainID, 
        }).then(gasPrice => {
          const networkGasPrice = gasPrice;
          let increasedGasPrice;
          if(networkGasPrice){
            increasedGasPrice = networkGasPrice + parseGwei('5')
          }
          else{
            increasedGasPrice = parseGwei('30')
          }
          const request = {
            address: payToRankUpContract.contractAddress as `0x${string}`,
            abi: payToRankUpContract.abi,
            functionName: 'ArchonPass',
            args: [_userID],
            gasPrice: increasedGasPrice,
            chainId: chainID
          };
          simulateContract(config, request).then(()=> {
            writeContract(config, request).then(transactionHash => {
              waitForTransactionReceipt(config, {
                hash: transactionHash,
              }).then(result => {
                resolve(result.transactionHash) 
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

    async callMasterPass(_userID: string): Promise<string | undefined> {
      const account = getAccount(config);
      if(account.status != "connected"){
        await reconnect(config)
      }
      console.log(getAccount(config))
      return new Promise<string | undefined>((resolve, reject) => {
        getGasPrice(config, {
          chainId: chainID, 
        }).then(gasPrice => {
          const networkGasPrice = gasPrice;
          let increasedGasPrice;
          if(networkGasPrice){
            increasedGasPrice = networkGasPrice + parseGwei('5')
          }
          else{
            increasedGasPrice = parseGwei('30')
          }
          const request = {
            address: payToRankUpContract.contractAddress as `0x${string}`,
            abi: payToRankUpContract.abi,
            functionName: 'MasterPass',
            args: [_userID],
            gasPrice: increasedGasPrice,
            chainId: chainID
          };
          simulateContract(config, request).then(()=> {
            writeContract(config, request).then(transactionHash => {
              waitForTransactionReceipt(config, {
                hash: transactionHash,
              }).then(result => {
                resolve(result.transactionHash) 
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

    async callHasPaidArchonPassList(_userID: string): Promise<boolean | undefined> {
      return new Promise<boolean | undefined>((resolve, ) => {
        readContract(config,{
          address: payToRankUpContract.contractAddress as `0x${string}`,
          abi: payToRankUpContract.abi,
          functionName: 'hasPaidArchonPassList',
          args: [_userID],
        }).then(data => {
          resolve(data as boolean)
        })
      })
    }

    async callHasPaidMasterPassList(_userID: string): Promise<boolean | undefined> {
      return new Promise<boolean | undefined>((resolve, ) => {
        readContract(config,{
          address: payToRankUpContract.contractAddress as `0x${string}`,
          abi: payToRankUpContract.abi,
          functionName: 'hasPaidMasterPassList',
          args: [_userID],
        }).then(data => {
          resolve(data as boolean)
        })
      })
    }

    private handleError(error: BaseError) {
    this.alertService.addAlert(`Error: ${error.message}`, "error");
  }
    
}
