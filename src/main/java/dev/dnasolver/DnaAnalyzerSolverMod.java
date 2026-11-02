package dev.dnasolver;

import net.fabricmc.api.ClientModInitializer;

public final class DnaAnalyzerSolverMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DnaConfig.get();
    }
}
