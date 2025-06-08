package Project_Noir.Athena.Service;

import Project_Noir.Athena.Model.ContractFunctionEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class GasLimitService {

    public BigInteger getGasLimit(ContractFunctionEnum contractFunctionEnum, int listOfBuyersSize){
        switch (contractFunctionEnum){
            case CreateNewAuction, AddContentCreator -> {
                return BigInteger.valueOf(193000L);
            }
            case ReturnBid -> {
                return BigInteger.valueOf(37000L);
            }
            case SendMana -> {
                return BigInteger.valueOf(38000L);
            }
            case SetAuctionToActive, ReactivateContent -> {
                return BigInteger.valueOf(13000L).multiply(BigInteger.valueOf(listOfBuyersSize));
            }
            case AddChannel -> {
                return BigInteger.valueOf(144000L);
            }
            case PayForContent -> {
                return BigInteger.valueOf(82000L);
            }
            case SendRefundPayment, CancelPayment -> {
                return BigInteger.valueOf(39000L);
            }
            case UpdatePersonalWallet, IncreaseCreatorRank, SetAuctionToInactive, UpdateAverageWeeklyViewers -> {
                return BigInteger.valueOf(28000L);
            }
            case SendWeeklyMana -> {
                return BigInteger.valueOf(40000L);
            }
            case WatchNowPayLater -> {
                return BigInteger.valueOf(195000L);
            }
            case WatchNowPayLaterPayment -> {
                return BigInteger.valueOf(34000L);
            }
            case SendWatchNowPayLaterRefundPayment, CancelWatchNowPayLater -> {
                return BigInteger.valueOf(51000L);
            }
        }
        return BigInteger.valueOf(500000L);
    }
}
