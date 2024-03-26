package org.example.app;

import jakarta.annotation.PostConstruct;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.Wallet;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WalletBalanceCron {
    final WalletAppKit walletAppKit;
    final MyWallet myWallet;
    AtomicInteger countTratsaction = new AtomicInteger();

    public WalletBalanceCron(WalletAppKit walletAppKit, MyWallet myWallet) {
        this.walletAppKit = walletAppKit;
        this.myWallet = myWallet;
    }
    @PostConstruct
    public void start() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleAtFixedRate(()-> {
            this.printWalletBalance();
            if(walletAppKit.wallet().getBalance().value > 300 && countTratsaction.get() < 1){
                myWallet.send();
                countTratsaction.getAndIncrement();
            }


        }, 0, 15, TimeUnit.SECONDS);

    }

    private void printWalletBalance() {
        Wallet wallet = walletAppKit.wallet();
        Coin balance = wallet.getBalance();
        System.out.println("Current wallet balance: " + balance.toFriendlyString());
    }
}
