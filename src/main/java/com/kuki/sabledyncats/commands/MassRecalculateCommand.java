package com.kuki.sabledyncats.commands;

import com.kuki.sabledyncats.CoolAssMethods.CopycatMassQueue;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only, on-demand, bounded reconciliation of loaded Sable ships. */
public final class MassRecalculateCommand {
    private MassRecalculateCommand() { }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sabledyncats")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("mass")
                        .then(Commands.literal("recalc_loaded").executes(ctx -> {
                            int count = CopycatMassQueue.startManualRecalculation(ctx.getSource().getServer());
                            if (count < 0) {
                                ctx.getSource().sendFailure(Component.literal(
                                        "A DynoCats mass reconciliation is already in progress. Use /sabledyncats mass status"));
                                return 0;
                            }
                            if (count == 0) {
                                ctx.getSource().sendFailure(Component.literal(
                                        "No loaded Sable ships found. Unloaded ships are never force-loaded."));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "DynoCats: queued inspection of " + count
                                    + " loaded Sable ships. Processing is limited per server tick."
                                    + " Use /sabledyncats mass status to check progress."), true);
                            return count;
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            String text = CopycatMassQueue.manualStatus();
                            ctx.getSource().sendSuccess(() -> Component.literal(text), false);
                            return 1;
                        }))));
    }
}
