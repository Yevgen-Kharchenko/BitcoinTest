package org.example.app;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;


@Component
public class MyWallet {

    @Autowired
    private WalletAppKit walletAppKit;

    @Autowired
    private NetworkParameters networkParameters;

    @Value("${bitcoin.address-to-send}")
    private String addressToSend;

    @PostConstruct
    public void start() throws IOException, MnemonicException.MnemonicLengthException {

        // Generation of a mnemonic phrase
        List<String> mnemonicCode = new MnemonicCode().toMnemonic(new SecureRandom().generateSeed(16));

        // Creating a wallet from a mnemonic phrase
        DeterministicSeed seed = new DeterministicSeed(mnemonicCode, null, "", 1409478661L);
//TODO
//        walletAppKit.restoreWalletFromSeed(seed);
        walletAppKit.startAsync();
        walletAppKit.awaitRunning();

        Address firstAddress = walletAppKit.wallet().freshReceiveAddress();
        System.out.println("********************************************************");
        System.out.println();
        System.out.println("Mnemonic code: " + String.join(" ", mnemonicCode));
        System.out.println("First address in wallet: " + firstAddress);
        System.out.println();
        System.out.println("********************************************************");

        walletAppKit.wallet().addCoinsReceivedEventListener(
                (wallet, tx, prevBalance, newBalance) -> {
                    Coin value = tx.getValueSentToMe(wallet);
                    System.out.println("Received tx for " + value.toFriendlyString());
                    Futures.addCallback(tx.getConfidence().getDepthFuture(1),
                            new FutureCallback<TransactionConfidence>() {
                                @Override
                                public void onSuccess(TransactionConfidence result) {
                                    System.out.println("Received tx " +
                                            value.toFriendlyString() + " is confirmed. ");
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                }
                            }, MoreExecutors.directExecutor());
                });

    }

    public void send() {

        try {
            Address toAddress = LegacyAddress.fromBase58(networkParameters, addressToSend);
            SendRequest sendRequest = SendRequest.to(toAddress, Coin.parseCoin("0.000002"));
            sendRequest.feePerKb = Coin.parseCoin("0.0000005");
            Wallet.SendResult sendResult = walletAppKit.wallet().sendCoins(walletAppKit.peerGroup(), sendRequest);
            sendResult.broadcastComplete.addListener(() -> {
                System.out.println("********************************************************");
                System.out.println();
                System.out.println("Sent coins onwards! Transaction hash is " + sendResult.tx.getTxId());
            }, MoreExecutors.directExecutor());

        } catch (InsufficientMoneyException e) {
            throw new RuntimeException(e);
        }
    }

}
