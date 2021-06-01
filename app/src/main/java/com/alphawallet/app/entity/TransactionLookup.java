package com.alphawallet.app.entity;

import com.alphawallet.app.R;

import java.util.HashMap;
import java.util.Map;

public class TransactionLookup
{
    private static Map<TransactionType, Integer> typeMapping = new HashMap<>();

    public static int typeToName(TransactionType type)
    {
        setupTypes();
        if (type.ordinal() > typeMapping.size()) return typeMapping.get(TransactionType.UNKNOWN);
        else return typeMapping.get(type);
    }

    public static String typeToEvent(TransactionType type)
    {
        switch (type)
        {
            case TRANSFER_FROM:
            case SEND:
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
                return "";
        }
    }

    public static int toFromText(TransactionType type)
    {
        switch (type)
        {
            case TRANSFER_TO:
                return R.string.to;
            case RECEIVED:
            case RECEIVE_FROM:
                return R.string.from_op;
            case APPROVE:
                return R.string.approve;
            default:
                return R.string.empty;
        }
    }

    private static void setupTypes()
    {
        if (typeMapping.size() == 0)
        {
            typeMapping.put(TransactionType.UNKNOWN, R.string.ticket_invalid_op);
            typeMapping.put(TransactionType.LOAD_NEW_TOKENS, R.string.ticket_load_new_tickets);
            typeMapping.put(TransactionType.TRANSFER_TO, R.string.ticket_transfer_to);
            typeMapping.put(TransactionType.RECEIVE_FROM, R.string.ticket_receive_from);
            typeMapping.put(TransactionType.CONSTRUCTOR, R.string.ticket_contract_constructor);
            typeMapping.put(TransactionType.TERMINATE_CONTRACT, R.string.ticket_terminate_contract);
            typeMapping.put(TransactionType.TRANSFER_FROM, R.string.ticket_transfer_from);
            typeMapping.put(TransactionType.ALLOCATE_TO, R.string.allocate_to);
            typeMapping.put(TransactionType.APPROVE, R.string.approve);
            typeMapping.put(TransactionType.RECEIVED, R.string.received);
            typeMapping.put(TransactionType.SEND, R.string.action_send);
            typeMapping.put(TransactionType.SEND_ETH, R.string.action_send_eth);
            typeMapping.put(TransactionType.WITHDRAW, R.string.action_withdraw);
            typeMapping.put(TransactionType.DEPOSIT, R.string.deposit);
            typeMapping.put(TransactionType.CONTRACT_CALL, R.string.contract_call);
            typeMapping.put(TransactionType.UNKNOWN_FUNCTION, R.string.contract_call);
        }
    }
}
