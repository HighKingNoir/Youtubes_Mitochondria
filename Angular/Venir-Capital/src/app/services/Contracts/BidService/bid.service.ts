import { Injectable } from '@angular/core';
import { bidServiceContract } from './bidServiceContract';
import { writeContract, simulateContract, waitForTransactionReceipt, getGasPrice } from '@wagmi/core'
import { BaseError, parseEther, parseGwei } from 'viem';
import { AlertService } from '../../Alerts/alert.service';
import { config } from '../config';
import { polygon } from '@wagmi/core/chains';
import { ganache } from '../ganache';
import { environment } from 'src/Environment/environment';

const chainID = environment.production ? 137 : 1337

@Injectable({
  providedIn: 'root'
})
export class BidService {
  web3:  any;

  constructor(
    private alertService: AlertService,
  ) { }

  async callPlaceBid(_contentID: string, _userID: string, _mana: string): Promise<string | undefined> {
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
          address: bidServiceContract.contractAddress as `0x${string}`,
          abi: bidServiceContract.abi,
          functionName: 'placeBid',
          args: [_contentID, _userID, parseEther(_mana)],
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



 
  async callRaiseBid(_contentID: string, _userID: string, _mana: string): Promise<string | undefined> {

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
          address: bidServiceContract.contractAddress as `0x${string}`,
          abi: bidServiceContract.abi,
          functionName: 'raiseBid',
          args: [_contentID, _userID, parseEther(_mana)],
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

  async callCancelBid(_contentID: string, _userID: string): Promise<string | undefined> {
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
          address: bidServiceContract.contractAddress as `0x${string}`,
          abi: bidServiceContract.abi,
          functionName: 'cancelBid',
          args: [_contentID, _userID],
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

  private handleError(error: BaseError) {
    this.alertService.addAlert(`Error: ${error.message}`, "error");
  }
}
  
