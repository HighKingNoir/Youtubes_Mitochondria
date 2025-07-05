import { Injectable } from '@angular/core';
import { AlertService } from '../../Alerts/alert.service';
import { readContract, waitForTransaction , writeContract , simulateContract, getGasPrice, waitForTransactionReceipt, getAccount, reconnect } from '@wagmi/core'
import { BaseError, formatEther, parseEther, parseGwei } from 'viem';
import { decentralandManaContract } from './decentralandManaContract';
import { environment } from 'src/Environment/environment';
import { config } from '../config';

type SupportedChainID = 137 | 1337;
const chainID: SupportedChainID = environment.production ? 137 : 1337;

@Injectable({
  providedIn: 'root'
})
export class DecentralandManaService {

  private bidServiceAddress: string = environment.Contract_Bid_Address
  private channelServiceAddress: string = environment.Contract_Channel_Address
  private decentralandManaAddress: string = environment.Contract_Decentraland_Mana_Address
  

  constructor(private alertService: AlertService) { }

  async approveBidService(_mana: string): Promise<string | undefined> {
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
          abi: decentralandManaContract.abi,
          address: this.decentralandManaAddress  as `0x${string}`,
          functionName: 'approve',
          args: [this.bidServiceAddress, parseEther(_mana)],
          gasPrice: increasedGasPrice,
          chainId: chainID
        };
      simulateContract(config, request).then(() => {
        writeContract(config, request).then(transactionHash => {
          waitForTransactionReceipt(config, {
            hash: transactionHash,
          }).then(result => {
            this.alertService.addAlert("Approval Complete", "success")
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

  async approveChannelService(_mana: string): Promise<string | undefined> {
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
          abi: decentralandManaContract.abi,
          address: this.decentralandManaAddress  as `0x${string}`,
          functionName: 'approve',
          args: [this.channelServiceAddress, parseEther(_mana)],
          gasPrice: increasedGasPrice,
          chainId: chainID
        };
        simulateContract(config, request).then(()=> {
          writeContract(config, request).then(transactionHash => {
            waitForTransactionReceipt(config, {
              hash: transactionHash,
            }).then(result => {
              this.alertService.addAlert("Approval Complete", "success")
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

  async getBidAllowance(_owner: string): Promise<number> {
    return new Promise<number>((resolve, ) => {
      readContract(config,{
        address: this.decentralandManaAddress  as `0x${string}`,
        abi: decentralandManaContract.abi,
        functionName: 'allowance',
        args: [_owner, this.bidServiceAddress],
      }).then(data => {
        resolve(Number(formatEther(data as bigint)))
      })
    })
  }

  async getChannelAllowance(_owner: string): Promise<number> {
    return new Promise<number>((resolve, ) => {
      readContract(config, {
        address: this.decentralandManaAddress  as `0x${string}`,
        abi: decentralandManaContract.abi,
        functionName: 'allowance',
        args: [_owner, this.channelServiceAddress],
      }).then(data => {
        resolve(Number(formatEther(data as bigint)))
      })
    })
  }


  async increaseAllowanceBidService( _mana: string): Promise<string | undefined> {
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
          abi: decentralandManaContract.abi,
          address: this.decentralandManaAddress  as `0x${string}`,
          functionName: 'increaseAllowance',
          args: [this.bidServiceAddress, parseEther(_mana)],
          gasPrice: increasedGasPrice,
          chainId: chainID
        };
        simulateContract(config, request).then(()=> {
          writeContract(config, request).then(transactionHash => {
            waitForTransactionReceipt(config, {
              hash: transactionHash,
            }).then(result => {
              this.alertService.addAlert("Increase Successful", "success")
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

  async increaseAllowanceChannelService(_mana: string): Promise<string | undefined> {
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
          abi: decentralandManaContract.abi,
          address: this.decentralandManaAddress  as `0x${string}`,
          functionName: 'increaseAllowance',
          args: [this.channelServiceAddress, parseEther(_mana)],
          gasPrice: increasedGasPrice,
          chainId: chainID
        };
        simulateContract(config, request).then(()=> {
          writeContract(config, request).then(transactionHash => {
            waitForTransactionReceipt(config, {
              hash: transactionHash,
            }).then(result => {
              this.alertService.addAlert("Increase Successful", "success")
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

  async decreaseAllowanceBidService( _mana: string): Promise<string | undefined> {
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
          abi: decentralandManaContract.abi,
          address: this.decentralandManaAddress  as `0x${string}`,
          functionName: 'decreaseAllowance',
          args: [this.bidServiceAddress, parseEther(_mana)],
          gasPrice: increasedGasPrice,
          chainId: chainID
        };
        simulateContract(config, request).then(()=> {
          writeContract(config, request).then(transactionHash => {
            waitForTransactionReceipt(config, {
              hash: transactionHash,
            }).then(result => {
              this.alertService.addAlert("Decrease Successful", "success")
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

  async decreaseAllowanceChannelService(_mana: string): Promise<string | undefined> {
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
          abi: decentralandManaContract.abi,
          address: this.decentralandManaAddress  as `0x${string}`,
          functionName: 'decreaseAllowance',
          args: [this.channelServiceAddress, parseEther(_mana)],
          gasPrice: increasedGasPrice,
          chainId: chainID
        };
        simulateContract(config, request).then(() => {
          writeContract(config, request).then(transactionHash => {
            waitForTransactionReceipt(config, {
              hash: transactionHash,
            }).then(result => {
              this.alertService.addAlert("Decrease Successful", "success")
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
