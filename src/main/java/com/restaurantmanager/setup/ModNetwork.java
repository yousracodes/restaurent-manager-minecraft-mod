package com.restaurantmanager.network;

import com.restaurantmanager.network.packet.OpenRestaurantManagementPacket;
import com.restaurantmanager.network.packet.OpenRestaurantMenuPacket;
import com.restaurantmanager.network.packet.OpenCustomerSuggestionPacket;
import com.restaurantmanager.network.packet.ApplyRestaurantMenuItemPacket;
import com.restaurantmanager.network.packet.RenameRestaurantPacket;
import com.restaurantmanager.network.packet.RequestRestaurantMenuPacket;
import com.restaurantmanager.network.packet.SaveMenuItemPacket;
import com.restaurantmanager.network.packet.ServeCustomerPacket;
import com.restaurantmanager.network.packet.ToggleRestaurantOpenPacket;
import com.restaurantmanager.network.packet.UpdateRestaurantMenuPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import com.restaurantmanager.RestaurantManagerMod;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;
    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void setup() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(RestaurantManagerMod.MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        CHANNEL.registerMessage(
                nextId(),
                ServeCustomerPacket.class,
                ServeCustomerPacket::encode,
                ServeCustomerPacket::decode,
                ServeCustomerPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                ToggleRestaurantOpenPacket.class,
                ToggleRestaurantOpenPacket::encode,
                ToggleRestaurantOpenPacket::decode,
                ToggleRestaurantOpenPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                OpenRestaurantManagementPacket.class,
                OpenRestaurantManagementPacket::encode,
                OpenRestaurantManagementPacket::decode,
                OpenRestaurantManagementPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                OpenCustomerSuggestionPacket.class,
                OpenCustomerSuggestionPacket::encode,
                OpenCustomerSuggestionPacket::decode,
                OpenCustomerSuggestionPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                RequestRestaurantMenuPacket.class,
                RequestRestaurantMenuPacket::encode,
                RequestRestaurantMenuPacket::decode,
                RequestRestaurantMenuPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                OpenRestaurantMenuPacket.class,
                OpenRestaurantMenuPacket::encode,
                OpenRestaurantMenuPacket::decode,
                OpenRestaurantMenuPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                UpdateRestaurantMenuPacket.class,
                UpdateRestaurantMenuPacket::encode,
                UpdateRestaurantMenuPacket::decode,
                UpdateRestaurantMenuPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                SaveMenuItemPacket.class,
                SaveMenuItemPacket::encode,
                SaveMenuItemPacket::decode,
                SaveMenuItemPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                RenameRestaurantPacket.class,
                RenameRestaurantPacket::encode,
                RenameRestaurantPacket::decode,
                RenameRestaurantPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                ApplyRestaurantMenuItemPacket.class,
                ApplyRestaurantMenuItemPacket::encode,
                ApplyRestaurantMenuItemPacket::decode,
                ApplyRestaurantMenuItemPacket::handle
        );
    }
}
