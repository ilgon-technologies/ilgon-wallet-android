package com.alphawallet.app.entity;

import android.content.Context;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.ENSHandler;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.BalanceUtils;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.alphawallet.app.C.BURN_ADDRESS;
import static com.alphawallet.app.C.ETHER_DECIMALS;
import static com.alphawallet.app.ui.widget.holder.TransactionHolder.TRANSACTION_BALANCE_PRECISION;

/**
 * Created by James on 4/03/2018.
 *
 * A data structure that consists part of the transaction: it has the
 * parameters for a function call (currently, only transactions to a
 * contract is dealt in this class) and it is only returned by using
 * TransactionDecoder. Note that the address of the contract, the name
 * of the function called and the signature from transaction sender
 * are all not in this class.
 *
 */

public class TransactionInput
{
    public FunctionData functionData;
    public List<String> addresses;
    public List<BigInteger> arrayValues;
    public List<String> sigData;
    public List<String> miscData;
    public String tradeAddress;
    public TransactionType type;

    private static final String ERC20_APPROVE_ALL = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    private static final BigDecimal ERC20_APPROVE_ALL_BD = BigDecimal.valueOf(9.999).movePointRight(17);

    public TransactionInput()
    {
        arrayValues = new ArrayList<>();
        addresses = new ArrayList<>();
        sigData = new ArrayList<>();
        miscData = new ArrayList<>();
    }

    //Addresses are in 256bit format
    public boolean containsAddress(String address)
    {
        boolean hasAddr = false;
        //Scan addresses for this address
        address = Numeric.cleanHexPrefix(address);
        for (String thisAddr : addresses)
        {
            if (thisAddr != null && thisAddr.contains(address))
            {
                hasAddr = true;
                break;
            }
        }

        return hasAddr;
    }

    public String getFirstAddress() {
        String address = "";
        if (addresses.size() > 0)
        {
            address = addresses.get(0);
        }
        return address;
    }

    public String getDestinationAddress()
    {
        String address = "";
        if (addresses.size() > 0)
        {
            address = getFirstAddress();
            switch (functionData.functionName)
            {
                case "transferFrom":
                case "safeTransferFrom":
                    if (addresses.size() > 1)
                    {
                        address = addresses.get(1); //destination address will be second address
                    }
                    break;
                default:
                    break;
            }
        }
        return address;
    }

    public String getAddress(int index)
    {
        String address = "";
        if (addresses.size() > index)
        {
            address = addresses.get(index);
        }

        return address;
    }

    //With static string as we don't have app context
    String getFirstValue()
    {
        String value = "0";
        if (miscData.size() > 0)
        {
            String firstVal = miscData.get(0);
            if (firstVal.equals(ERC20_APPROVE_ALL))
            {
                value = "All"; //Can't localise here because no App context.
            }
            else
            {
                //convert to big integer
                BigInteger bi = new BigInteger(miscData.get(0), 16);
                value = bi.toString(10);
            }
        }
        return value;
    }

    private String getFirstValueScaled(Context ctx, int tokenDecimal)
    {
        String value = "0";
        if (miscData.size() > 0)
        {
            String firstVal = miscData.get(0);
            BigInteger bi = new BigInteger(firstVal, 16);
            BigDecimal scaledVal = new BigDecimal(bi).divide(BigDecimal.valueOf(Math.pow(10, tokenDecimal)), 18, RoundingMode.DOWN);
            if (firstVal.equals(ERC20_APPROVE_ALL) || scaledVal.compareTo(ERC20_APPROVE_ALL_BD) >= 0)
            {
                if (ctx != null)
                {
                    value = ctx.getString(R.string.all);
                }
                else
                {
                    value = "All";
                }
            }
            else
            {
                //convert to big integer
                value = BalanceUtils.getScaledValueMinimal(new BigDecimal(bi),
                        tokenDecimal, 4);
            }
        }
        return value;
    }

    public String getOperationTitle(Context ctx)
    {
        return ctx.getString(TransactionLookup.typeToName(type));
    }

    public int getOperationToFrom()
    {
        switch (type)
        {
            case TRANSFER_TO:
            case SEND:
                return R.string.to;
            case RECEIVED:
            case RECEIVE_FROM:
                return R.string.from_op;
            case APPROVE:
                return R.string.approve;
            default:
                return 0;
        }
    }

    public String getOperationDescription(Context ctx, Transaction tx, Token t, TokensService tService)
    {
        String operation = "";
        switch (type)
        {
            case APPROVE: //show address as token name, ENS name or formatted address
                String amount = getFirstValueScaled(ctx, t.tokenInfo.decimals);
                String approveAddr = getFirstAddress();
                Token approveToken = tService.getToken(tx.chainId, approveAddr);
                approveAddr = approveToken == null ? ENSHandler.matchENSOrFormat(ctx, approveAddr) : approveToken.getShortName();
                operation = ctx.getString(R.string.default_approve, amount, t.getSymbol(), approveAddr);
                break;
            case TERMINATE_CONTRACT:
                operation = ENSHandler.matchENSOrFormat(ctx, tx.to);
                break;
            case CONSTRUCTOR:
                operation = t.getFullName();
                break;
            default: //use default definition
                int operationResId = getOperationToFrom();
                if (operationResId != 0)
                {
                    operation = ctx.getString(R.string.operation_definition, ctx.getString(operationResId), ENSHandler.matchENSOrFormat(ctx, getOperationAddress(tx, t)));
                }
                else
                {
                    operation = ENSHandler.matchENSOrFormat(ctx, tx.to);
                }
                break;
        }

        return operation;
    }

    public String getOperationAddress(Transaction tx, Token t)
    {
        String address = tx.to;

        switch (type)
        {
            case TRANSFER_TO:
                break;
            case SEND:
                address = getDestinationAddress();
                break;
            case RECEIVE_FROM:
                break;
            case TRANSFER_FROM:
                address = tx.from;
                break;
            case ALLOCATE_TO:
                address = getFirstAddress();
                break;
            case APPROVE:
                address = getFirstAddress();
                break;
            case RECEIVED:
                address = tx.from;
                break;
            case LOAD_NEW_TOKENS:
            case CONSTRUCTOR:
            case TERMINATE_CONTRACT:
                address = t.getAddress();
                break;
            case UNKNOWN_FUNCTION:
            case INVALID_OPERATION:
            default:
                address = tx.from;
                break;
        }

        return address;
    }


    /*
    addFunction("transferFrom(address,address,uint16[])", ContractType.ERC875_LEGACY, false);
        addFunction("transfer(address,uint16[])", ContractType.ERC875_LEGACY, false);
        addFunction("trade(uint256,uint16[],uint8,bytes32,bytes32)", ContractType.ERC875_LEGACY, true);
        addFunction("passTo(uint256,uint16[],uint8,bytes32,bytes32,address)", ContractType.ERC875_LEGACY, true);
        addFunction("loadNewTickets(bytes32[])", ContractType.ERC875_LEGACY, false);
        addFunction("balanceOf(address)", ContractType.ERC875_LEGACY, false);

        addFunction("transfer(address,uint256)", ContractType.ERC20, false);
        addFunction("transfer(address,uint)", ContractType.ERC20, false);
        addFunction("transferFrom(address,address,uint256)", ContractType.ERC20, false);
        addFunction("approve(address,uint256)", ContractType.ERC20, false);
        addFunction("approve(address,uint)", ContractType.ERC20, false);
        addFunction("allocateTo(address,uint256)", ContractType.ERC20, false);
        addFunction("allowance(address,address)", ContractType.ERC20, false);
        addFunction("transferFrom(address,address,uint)", ContractType.ERC20, false);
        addFunction("approveAndCall(address,uint,bytes)", ContractType.ERC20, false);
        addFunction("balanceOf(address)", ContractType.ERC20, false);
        addFunction("transferAnyERC20Token(address,uint)", ContractType.ERC20, false);
        addFunction("delegate(address)", ContractType.ERC20, false);
        addFunction("mint(address,uint)", ContractType.ERC20, false);

        addFunction("transferFrom(address,address,uint256[])", ContractType.ERC875, false);
        addFunction("transfer(address,uint256[])", ContractType.ERC875, false);
        addFunction("trade(uint256,uint256[],uint8,bytes32,bytes32)", ContractType.ERC875, true);
        addFunction("passTo(uint256,uint256[],uint8,bytes32,bytes32,address)", ContractType.ERC875, true);
        addFunction("loadNewTickets(uint256[])", ContractType.ERC875, false);
        addFunction("balanceOf(address)", ContractType.ERC875, false);

        addFunction("endContract()", ContractType.CREATION, false);
        addFunction("selfdestruct()", ContractType.CREATION, false);
        addFunction("kill()", ContractType.CREATION, false);

        addFunction("safeTransferFrom(address,address,uint256,bytes)", ContractType.ERC721, false);
        addFunction("safeTransferFrom(address,address,uint256)", ContractType.ERC721, false);
        addFunction("transferFrom(address,address,uint256)", ContractType.ERC721, false);
        addFunction("approve(address,uint256)", ContractType.ERC721, false);
        addFunction("setApprovalForAll(address,bool)", ContractType.ERC721, false);
        addFunction("getApproved(address,address,uint256)", ContractType.ERC721, false);
        addFunction("isApprovedForAll(address,address)", ContractType.ERC721, false);
        addFunction("transfer(address,uint256)", ContractType.ERC721_LEGACY, false);
        addFunction("giveBirth(uint256,uint256)", ContractType.ERC721, false);
        addFunction("breedWithAuto(uint256,uint256)", ContractType.ERC721, false);
        addFunction("ownerOf(uint256)", ContractType.ERC721, false);
        addFunction("createSaleAuction(uint256,uint256,uint256,uint256)", ContractType.ERC721, false);
        addFunction("mixGenes(uint256,uint256,uint256)", ContractType.ERC721, false);
        addFunction("tokensOfOwner(address)", ContractType.ERC721, false);

        addFunction("dropCurrency(uint32,uint32,uint32,uint8,bytes32,bytes32,address)", ContractType.CURRENCY, true);
        addFunction("withdraw(uint256)", ContractType.CURRENCY, false);
     */

    public void setOperationType(Transaction tx, String walletAddress)
    {
        if (tx != null && (tx.isConstructor ||
                (tx.input != null && tx.input.equals(Transaction.CONSTRUCTOR)))) //input labelled as constructor
        {
            type = TransactionType.CONSTRUCTOR;
            return;
        }

        if (tx != null && tx.input != null && tx.input.equals("0x"))
        {
            type = TransactionType.SEND_ETH;
            return;
        }

        switch (functionData.functionName)
        {
            case "safeTransferFrom":
            case "transferFrom":
                type = interpretTransferFrom(walletAddress);
                break;
            case "transfer":
                type = interpretTransfer(walletAddress);
                break;
            case "allocateTo":
                type = TransactionType.ALLOCATE_TO;
                break;
            case "approve":
                type = TransactionType.APPROVE;
                break;
            case "loadNewTickets":
                type = TransactionType.LOAD_NEW_TOKENS;
                break;
            case "endContract":
            case "selfdestruct":
            case "kill":
                type = TransactionType.TERMINATE_CONTRACT;
                break;
            case "withdraw":
                type = TransactionType.WITHDRAW;
                break;
            case "deposit":
                type = TransactionType.DEPOSIT;
                break;
            default:
                type = TransactionType.CONTRACT_CALL;
                break;
        }
    }



    private TransactionType interpretTransferFrom(String walletAddr)
    {
        String destinationAddr = getDestinationAddress();
        if (walletAddr == null)
        {
            return TransactionType.TRANSFER_FROM;
        }
        else if (!destinationAddr.equalsIgnoreCase(walletAddr)) //otherparty in this case will be the first address, the previous owner of the token(s)
        {
            return TransactionType.TRANSFER_FROM;
        }
        else
        {
            return TransactionType.TRANSFER_TO;
        }
    }

    private TransactionType interpretTransfer(String walletAddr)
    {
        if (walletAddr == null)
        {
            return TransactionType.TRANSFER_TO;
        }
        //this could be transfer to or from
        //if destination is our address then it must be a receive
        else if (getDestinationAddress().equalsIgnoreCase(walletAddr))
        {
            return TransactionType.RECEIVED;// R.string.ticket_receive_from;
        }
        else
        {
            return TransactionType.SEND;
        }
    }

    public String getOperationValue(Token token, Transaction tx)
    {
        String operationValue = "";

        boolean addSymbol = true;

        switch (type)
        {
            case APPROVE:
            case ALLOCATE_TO:
                operationValue = getFirstValueScaled(null, token.tokenInfo.decimals);
                break;
            case TRANSFER_TO:
            case RECEIVE_FROM:
            case TRANSFER_FROM:
            case RECEIVED:
            case SEND:
                operationValue = token.getTransferValue(tx.transactionInput, TRANSACTION_BALANCE_PRECISION);
                break;

            case LOAD_NEW_TOKENS:
                operationValue = String.valueOf(arrayValues.size());
                break;
            case CONSTRUCTOR:
            case TERMINATE_CONTRACT:
                operationValue = "";
                break;
            case CONTRACT_CALL:
                addSymbol = false;
                break;
            case WITHDRAW:
                addSymbol = false;
                break;
            case DEPOSIT:
                addSymbol = false;
                break;
            case UNKNOWN_FUNCTION:
            case INVALID_OPERATION:
            default:
                operationValue = "";
                break;
        }

        if (addSymbol)
        {
            if (!token.isEthereum())
            {
                operationValue = operationValue + " " + token.getSymbol();
            }
            else
            {
                operationValue = "";
            }
        }

        return operationValue;
    }

    public boolean shouldShowSymbol(Token token)
    {
        switch (type)
        {
            case CONTRACT_CALL:
            case WITHDRAW:
                return false;
            default:
                return !token.isEthereum(); //can't be ethereum if has transaction input
        }
    }

    public StatusType getOperationImage(Transaction tx, String walletAddress)
    {
        switch (type)
        {
            case SEND:
                return StatusType.SENT;
            case TRANSFER_TO:
                return StatusType.RECEIVE;
            case RECEIVE_FROM:
                return StatusType.SENT;
            case TRANSFER_FROM:
                return StatusType.SENT;
            case APPROVE:
                return StatusType.SENT;
            case RECEIVED:
                return StatusType.RECEIVE;
            case ALLOCATE_TO:
                return StatusType.SENT;

            case LOAD_NEW_TOKENS:
            case CONSTRUCTOR:
            case TERMINATE_CONTRACT:
                return StatusType.CONSTRUCTOR;
            default:
                return tx.from.equalsIgnoreCase(walletAddress) ? (tx.to.equalsIgnoreCase(tx.from) ? StatusType.SELF : StatusType.SENT)
                    : StatusType.RECEIVE;
        }
    }

    public BigDecimal getRawValue()
    {
        //this is for a transfer etc.
        if (arrayValues.size() > 0)
        {
            return new BigDecimal(arrayValues.size());
        }
        else
        {
            //get first value
            return new BigDecimal(getFirstValue());
        }
    }

    public String getOperationEvent(String walletAddress)
    {
        switch (type)
        {
            case SEND:
            case TRANSFER_FROM:
                return "sent";
            case TRANSFER_TO:
                return "received";
            case RECEIVE_FROM:
                return "received";
            case RECEIVED:
                return "received";
            case APPROVE:
                return "ownerApproved";
            default:
                if (getDestinationAddress().equalsIgnoreCase(walletAddress))
                {
                    return "received";
                }
                else
                {
                    return "sent";
                }
        }
    }

    public boolean isSent()
    {
        switch (type)
        {
            case SEND:
                return true;
            case TRANSFER_TO:
                return false;
            case RECEIVE_FROM:
                return true;
            case TRANSFER_FROM:
                return true;
            case APPROVE:
                return true;
            case RECEIVED:
                return false;
            case ALLOCATE_TO:
                return true;
            default:
                return true;
        }
    }

    public boolean isSendOrReceive(Transaction tx)
    {
        switch (type)
        {
            case SEND:
            case TRANSFER_TO:
            case RECEIVE_FROM:
            case TRANSFER_FROM:
            case RECEIVED:
                return true;
            default:
                return !tx.value.equals("0");
        }
    }
}
