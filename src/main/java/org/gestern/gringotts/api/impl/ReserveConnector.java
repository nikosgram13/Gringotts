package org.gestern.gringotts.api.impl;

import net.tnemc.core.economy.EconomyAPI;
import net.tnemc.core.economy.currency.Currency;
import net.tnemc.core.economy.response.AccountResponse;
import net.tnemc.core.economy.response.CustomResponse;
import net.tnemc.core.economy.response.EconomyResponse;
import net.tnemc.core.economy.response.GeneralResponse;
import net.tnemc.core.economy.response.HoldingsResponse;
import org.bukkit.World;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.Language;
import org.gestern.gringotts.api.Account;
import org.gestern.gringotts.api.Eco;
import org.gestern.gringotts.api.TransactionResult;

import java.math.BigDecimal;
import java.util.UUID;

public class ReserveConnector implements EconomyAPI {

  private final Eco eco = Gringotts.getInstance().getEco();

  @Override
  public String name() {
    return "Gringotts";
  }

  @Override
  public String version() {
    return "0.1.4.6";
  }
  /**
   * @return Whether or not this implementation is enabled.
   */
  @Override
  public boolean enabled() {
    return true;
  }

  /**
   * @return Whether or not this implementation should have a default Vault implementation.
   */
  @Override
  public boolean vault() {
    return false;
  }

  /**
   * Used to get the plural name of the default currency.
   * @return The plural name of the default currency.
   */
  @Override
  public String currencyDefaultPlural() {
    return eco.currency().namePlural();
  }

  /**
   * Used to get the singular name of the default currency.
   * @return The plural name of the default currency.
   */
  @Override
  public String currencyDefaultSingular() {
    return eco.currency().name();
  }

  /**
   * Used to get the plural name of the default currency for a world.
   * @param world The world to be used in this check.
   * @return The plural name of the default currency.
   */
  @Override
  public String currencyDefaultPlural(String world) {
    return currencyDefaultPlural();
  }

  /**
   * Used to get the singular name of the default currency for a world.
   * @param world The world to be used in this check.
   * @return The plural name of the default currency.
   */
  @Override
  public String currencyDefaultSingular(String world) {
    return currencyDefaultSingular();
  }

  /**
   * Checks to see if a {@link Currency} exists with this name.
   * @param name The name of the {@link Currency} to search for.
   * @return True if the currency exists, else false.
   */
  @Override
  public boolean hasCurrency(String name) {
    return name.equals(currencyDefaultSingular());
  }

  /**
   * Checks to see if a {@link Currency} exists with this name.
   * @param name The name of the {@link Currency} to search for.
   * @param world The name of the {@link World} to check for this {@link Currency} in.
   * @return True if the currency exists, else false.
   */
  @Override
  public boolean hasCurrency(String name, String world) {
    return name.equals(currencyDefaultSingular());
  }

  /**
   * Checks to see if an account exists for this identifier. This method should be used for non-player accounts.
   * @param identifier The identifier of the account.
   * @return True if an account exists for this identifier, else false.
   */
  @Override
  public EconomyResponse hasAccountDetail(String identifier) {
    if (eco.account(identifier).exists()) {
      return GeneralResponse.SUCCESS;
    }
    return AccountResponse.DOESNT_EXIST;
  }

  /**
   * Checks to see if an account exists for this identifier. This method should be used for player accounts.
   * @param identifier The {@link UUID} of the account.
   * @return True if an account exists for this identifier, else false.
   */
  @Override
  public EconomyResponse hasAccountDetail(UUID identifier) {
    if (eco.player(identifier).exists()) {
      return GeneralResponse.SUCCESS;
    }
    return AccountResponse.DOESNT_EXIST;
  }

  /**
   * Attempts to create an account for this identifier. This method should be used for non-player accounts.
   * @param identifier The identifier of the account.
   * @return True if an account was created, else false.
   */
  @Override
  public EconomyResponse createAccountDetail(String identifier) {
    if (eco.account(identifier).exists()) return AccountResponse.ALREADY_EXISTS;
    if (eco.account(identifier).create() != null) {
      return GeneralResponse.SUCCESS;
    }
    return GeneralResponse.FAILED;
  }

  /**
   * Attempts to create an account for this identifier. This method should be used for player accounts.
   * @param identifier The {@link UUID} of the account.
   * @return True if an account was created, else false.
   */
  @Override
  public EconomyResponse createAccountDetail(UUID identifier) {
    if (eco.player(identifier).exists()) return AccountResponse.ALREADY_EXISTS;
    if (eco.player(identifier).create() != null) {
      return GeneralResponse.SUCCESS;
    }
    return GeneralResponse.FAILED;
  }

  /**
   * Attempts to delete an account for this identifier. This method should be used for non-player accounts.
   * @param identifier The identifier of the account.
   * @return True if an account was deleted, else false.
   */
  @Override
  public EconomyResponse deleteAccountDetail(String identifier) {
   if (!hasAccount(identifier)) return AccountResponse.DOESNT_EXIST;
   eco.account(identifier).delete();
   return GeneralResponse.SUCCESS;
  }

  /**
   * Attempts to delete an account for this identifier. This method should be used for player accounts.
   * @param identifier The {@link UUID} of the account.
   * @return True if an account was deleted, else false.
   */
  @Override
  public EconomyResponse deleteAccountDetail(UUID identifier) {
    if (!hasAccount(identifier)) return AccountResponse.DOESNT_EXIST;
    eco.player(identifier).delete();
    return GeneralResponse.SUCCESS;
  }

  /**
   * Determines whether or not a player is able to access this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return Whether or not the player is able to access this account.
   */
  @Override
  public boolean isAccessor(String identifier, String accessor) {
    return identifier.equals(accessor);
  }

  /**
   * Determines whether or not a player is able to access this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return Whether or not the player is able to access this account.
   */
  @Override
  public boolean isAccessor(String identifier, UUID accessor) {
    return eco.account(identifier).id().equals(eco.player(accessor).id());
  }

  /**
   * Determines whether or not a player is able to access this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return Whether or not the player is able to access this account.
   */
  @Override
  public boolean isAccessor(UUID identifier, String accessor) {
    return eco.player(identifier).id().equals(eco.account(accessor).id());
  }

  /**
   * Determines whether or not a player is able to access this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return Whether or not the player is able to access this account.
   */
  @Override
  public boolean isAccessor(UUID identifier, UUID accessor) {
    return eco.player(identifier).id().equals(eco.player(accessor).id());
  }

  /**
   * Determines whether or not a player is able to withdraw holdings from this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canWithdrawDetail(String identifier, String accessor) {
    if (eco.account(identifier).id().equals(eco.account(accessor).id())) {
      return GeneralResponse.SUCCESS;
    }
    return GeneralResponse.FAILED;
  }

  /**
   * Determines whether or not a player is able to withdraw holdings from this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canWithdrawDetail(String identifier, UUID accessor) {
    if (eco.account(identifier).id().equals(eco.player(accessor).id())) {
      return GeneralResponse.SUCCESS;
    }
    return GeneralResponse.FAILED;
  }

  /**
   * Determines whether or not a player is able to withdraw holdings from this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canWithdrawDetail(UUID identifier, String accessor) {
    if (eco.player(identifier).id().equals(eco.account(accessor).id())) {
      return GeneralResponse.SUCCESS;
    }
    return GeneralResponse.FAILED;
  }

  /**
   * Determines whether or not a player is able to withdraw holdings from this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canWithdrawDetail(UUID identifier, UUID accessor) {
    if (eco.player(identifier).id().equals(eco.player(accessor).id())) {
      return GeneralResponse.SUCCESS;
    }
    return GeneralResponse.FAILED;
  }

  /**
   * Determines whether or not a player is able to deposit holdings into this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canDepositDetail(String identifier, String accessor) {
    if (eco.account(identifier).id().equals(eco.account(accessor).id())) {
      return GeneralResponse.SUCCESS;
    }
    return GeneralResponse.FAILED;
  }

  /**
   * Determines whether or not a player is able to deposit holdings into this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canDepositDetail(String identifier, UUID accessor) {
    if (eco.account(identifier).id().equals(eco.player(accessor).id())) {
      return GeneralResponse.SUCCESS;
    }
    return GeneralResponse.FAILED;
  }

  /**
   * Determines whether or not a player is able to deposit holdings into this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canDepositDetail(UUID identifier, String accessor) {
    if (eco.player(identifier).id().equals(eco.account(accessor).id())) {
      return GeneralResponse.SUCCESS;
    }
    return GeneralResponse.FAILED;
  }

  /**
   * Determines whether or not a player is able to deposit holdings into this account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param accessor The identifier of the user attempting to access this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canDepositDetail(UUID identifier, UUID accessor) {
    if (eco.player(identifier).id().equals(eco.player(accessor).id())) {
      return GeneralResponse.SUCCESS;
    }
    return GeneralResponse.FAILED;
  }

  /**
   * Used to get the balance of an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @return The balance of the account.
   */
  @Override
  public BigDecimal getHoldings(String identifier) {
    return new BigDecimal(eco.account(identifier).balance());
  }

  /**
   * Used to get the balance of an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @return The balance of the account.
   */
  @Override
  public BigDecimal getHoldings(UUID identifier) {
    return new BigDecimal(eco.player(identifier).balance());
  }

  /**
   * Used to get the balance of an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param world The name of the {@link World} associated with the balance.
   * @return The balance of the account.
   */
  @Override
  public BigDecimal getHoldings(String identifier, String world) {
    return getHoldings(identifier);
  }

  /**
   * Used to get the balance of an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param world The name of the {@link World} associated with the balance.
   * @return The balance of the account.
   */
  @Override
  public BigDecimal getHoldings(UUID identifier, String world) {
    return getHoldings(identifier);
  }

  /**
   * Used to get the balance of an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param world The name of the {@link World} associated with the balance.
   * @param currency The {@link Currency} associated with the balance.
   * @return The balance of the account.
   */
  @Override
  public BigDecimal getHoldings(String identifier, String world, String currency) {
    return getHoldings(identifier);
  }

  /**
   * Used to get the balance of an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param world The name of the {@link World} associated with the balance.
   * @param currency The {@link Currency} associated with the balance.
   * @return The balance of the account.
   */
  @Override
  public BigDecimal getHoldings(UUID identifier, String world, String currency) {
    return getHoldings(identifier);
  }

  /**
   * Used to determine if an account has at least an amount of funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to use for this check.
   * @return True if the account has at least the specified amount of funds, otherwise false.
   */
  @Override
  public boolean hasHoldings(String identifier, BigDecimal amount) {
    return eco.account(identifier).has(amount.doubleValue());
  }

  /**
   * Used to determine if an account has at least an amount of funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to use for this check.
   * @return True if the account has at least the specified amount of funds, otherwise false.
   */
  @Override
  public boolean hasHoldings(UUID identifier, BigDecimal amount) {
    return eco.player(identifier).has(amount.doubleValue());
  }

  /**
   * Used to determine if an account has at least an amount of funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to use for this check.
   * @param world The name of the {@link World} associated with the amount.
   * @return True if the account has at least the specified amount of funds, otherwise false.
   */
  @Override
  public boolean hasHoldings(String identifier, BigDecimal amount, String world) {
    return hasHoldings(identifier, amount);
  }

  /**
   * Used to determine if an account has at least an amount of funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to use for this check.
   * @param world The name of the {@link World} associated with the amount.
   * @return True if the account has at least the specified amount of funds, otherwise false.
   */
  @Override
  public boolean hasHoldings(UUID identifier, BigDecimal amount, String world) {
    return hasHoldings(identifier, amount);
  }

  /**
   * Used to determine if an account has at least an amount of funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to use for this check.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return True if the account has at least the specified amount of funds, otherwise false.
   */
  @Override
  public boolean hasHoldings(String identifier, BigDecimal amount, String world, String currency) {
    return hasHoldings(identifier, amount);
  }

  /**
   * Used to determine if an account has at least an amount of funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to use for this check.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return True if the account has at least the specified amount of funds, otherwise false.
   */
  @Override
  public boolean hasHoldings(UUID identifier, BigDecimal amount, String world, String currency) {
    return hasHoldings(identifier, amount);
  }

  /**
   * Used to set the funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to set this accounts's funds to.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse setHoldingsDetail(String identifier, BigDecimal amount) {
    return setHoldings(eco.account(identifier), amount);
  }

  /**
   * Used to set the funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to set this accounts's funds to.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse setHoldingsDetail(UUID identifier, BigDecimal amount) {
    return setHoldings(eco.player(identifier), amount);
  }

  /**
   * Used to set the funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to set this accounts's funds to.
   * @param world The name of the {@link World} associated with the amount.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse setHoldingsDetail(String identifier, BigDecimal amount, String world) {
    return setHoldingsDetail(identifier, amount);
  }

  /**
   * Used to set the funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to set this accounts's funds to.
   * @param world The name of the {@link World} associated with the amount.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse setHoldingsDetail(UUID identifier, BigDecimal amount, String world) {
    return setHoldingsDetail(identifier, amount);
  }

  /**
   * Used to set the funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to set this accounts's funds to.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse setHoldingsDetail(String identifier, BigDecimal amount, String world, String currency) {
    return setHoldingsDetail(identifier, amount);
  }

  /**
   * Used to set the funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to set this accounts's funds to.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse setHoldingsDetail(UUID identifier, BigDecimal amount, String world, String currency) {
    return setHoldingsDetail(identifier, amount);
  }

  /**
   * Used to add funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse addHoldingsDetail(String identifier, BigDecimal amount) {
    return addHoldings(eco.account(identifier), amount);
  }

  /**
   * Used to add funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse addHoldingsDetail(UUID identifier, BigDecimal amount) {
    return addHoldings(eco.player(identifier), amount);
  }

  /**
   * Used to add funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @param world The name of the {@link World} associated with the amount.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse addHoldingsDetail(String identifier, BigDecimal amount, String world) {
    return addHoldingsDetail(identifier, amount);
  }

  /**
   * Used to add funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @param world The name of the {@link World} associated with the amount.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse addHoldingsDetail(UUID identifier, BigDecimal amount, String world) {
    return addHoldingsDetail(identifier, amount);
  }

  /**
   * Used to add funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse addHoldingsDetail(String identifier, BigDecimal amount, String world, String currency) {
    return addHoldingsDetail(identifier, amount);
  }

  /**
   * Used to add funds to an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse addHoldingsDetail(UUID identifier, BigDecimal amount, String world, String currency) {
    return addHoldingsDetail(identifier, amount);
  }

  /**
   * Used to determine if a call to the corresponding addHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canAddHoldingsDetail(String identifier, BigDecimal amount) {
    if (!hasAccount(identifier)) return AccountResponse.DOESNT_EXIST;
    if (!hasAccount(identifier) && !createAccount(identifier)) return AccountResponse.CREATION_FAILED;

    return GeneralResponse.SUCCESS;
  }

  /**
   * Used to determine if a call to the corresponding addHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canAddHoldingsDetail(UUID identifier, BigDecimal amount) {
    if (!hasAccount(identifier)) return AccountResponse.DOESNT_EXIST;
    if (!hasAccount(identifier) && !createAccount(identifier)) return AccountResponse.CREATION_FAILED;

    return GeneralResponse.SUCCESS;
  }

  /**
   * Used to determine if a call to the corresponding addHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @param world The name of the {@link World} associated with the amount.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canAddHoldingsDetail(String identifier, BigDecimal amount, String world) {
    return canAddHoldingsDetail(identifier, amount);
  }

  /**
   * Used to determine if a call to the corresponding addHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @param world The name of the {@link World} associated with the amount.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canAddHoldingsDetail(UUID identifier, BigDecimal amount, String world) {
    return canAddHoldingsDetail(identifier, amount);
  }

  /**
   * Used to determine if a call to the corresponding addHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canAddHoldingsDetail(String identifier, BigDecimal amount, String world, String currency) {
    return canAddHoldingsDetail(identifier, amount);
  }

  /**
   * Used to determine if a call to the corresponding addHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to add to this account.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse canAddHoldingsDetail(UUID identifier, BigDecimal amount, String world, String currency) {
    return canAddHoldingsDetail(identifier, amount);
  }


  /**
   * Used to remove funds from an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse removeHoldingsDetail(String identifier, BigDecimal amount) {
    return takeHoldings(eco.account(identifier), amount);
  }

  /**
   * Used to remove funds from an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse removeHoldingsDetail(UUID identifier, BigDecimal amount) {
    return takeHoldings(eco.player(identifier), amount);
  }

  /**
   * Used to remove funds from an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @param world The name of the {@link World} associated with the amount.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse removeHoldingsDetail(String identifier, BigDecimal amount, String world) {
    return removeHoldingsDetail(identifier, amount);
  }

  /**
   * Used to remove funds from an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @param world The name of the {@link World} associated with the amount.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse removeHoldingsDetail(UUID identifier, BigDecimal amount, String world) {
    return removeHoldingsDetail(identifier, amount);
  }

  /**
   * Used to remove funds from an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse removeHoldingsDetail(String identifier, BigDecimal amount, String world, String currency) {
    return removeHoldingsDetail(identifier, amount);
  }

  /**
   * Used to remove funds from an account.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return The {@link EconomyResponse} for this action.
   */
  @Override
  public EconomyResponse removeHoldingsDetail(UUID identifier, BigDecimal amount, String world, String currency) {
    return removeHoldingsDetail(identifier, amount);
  }

  /**
   * Used to determine if a call to the corresponding removeHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @return The {@link EconomyResponse} that would be returned with the corresponding removeHoldingsDetail method.
   */
  @Override
  public EconomyResponse canRemoveHoldingsDetail(String identifier, BigDecimal amount) {
    if (!hasAccount(identifier)) return AccountResponse.DOESNT_EXIST;
    if (!hasAccount(identifier) && !createAccount(identifier)) return AccountResponse.CREATION_FAILED;
    if (hasHoldings(identifier, amount)) return HoldingsResponse.INSUFFICIENT;
    return GeneralResponse.SUCCESS;
  }

  /**
   * Used to determine if a call to the corresponding removeHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @return The {@link EconomyResponse} that would be returned with the corresponding removeHoldingsDetail method.
   */
  @Override
  public EconomyResponse canRemoveHoldingsDetail(UUID identifier, BigDecimal amount) {
    if (!hasAccount(identifier)) return AccountResponse.DOESNT_EXIST;
    if (!hasAccount(identifier) && !createAccount(identifier)) return AccountResponse.CREATION_FAILED;
    if (hasHoldings(identifier, amount)) return HoldingsResponse.INSUFFICIENT;
    return GeneralResponse.SUCCESS;
  }

  /**
   * Used to determine if a call to the corresponding removeHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @param world The name of the {@link World} associated with the amount.
   * @return The {@link EconomyResponse} that would be returned with the corresponding removeHoldingsDetail method.
   */
  @Override
  public EconomyResponse canRemoveHoldingsDetail(String identifier, BigDecimal amount, String world) {
    return canRemoveHoldingsDetail(identifier, amount);
  }

  /**
   * Used to determine if a call to the corresponding removeHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @param world The name of the {@link World} associated with the amount.
   * @return The {@link EconomyResponse} that would be returned with the corresponding removeHoldingsDetail method.
   */
  @Override
  public EconomyResponse canRemoveHoldingsDetail(UUID identifier, BigDecimal amount, String world) {
    return canRemoveHoldingsDetail(identifier, amount);
  }

  /**
   * Used to determine if a call to the corresponding removeHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return The {@link EconomyResponse} that would be returned with the corresponding removeHoldingsDetail method.
   */
  @Override
  public EconomyResponse canRemoveHoldingsDetail(String identifier, BigDecimal amount, String world, String currency) {
    return canRemoveHoldingsDetail(identifier, amount);
  }

  /**
   * Used to determine if a call to the corresponding removeHoldings method would be successful. This method does not
   * affect an account's funds.
   * @param identifier The identifier of the account that is associated with this call.
   * @param amount The amount you wish to remove from this account.
   * @param world The name of the {@link World} associated with the amount.
   * @param currency The {@link Currency} associated with the balance.
   * @return The {@link EconomyResponse} that would be returned with the corresponding removeHoldingsDetail method.
   */
  @Override
  public EconomyResponse canRemoveHoldingsDetail(UUID identifier, BigDecimal amount, String world, String currency) {
    return canRemoveHoldingsDetail(identifier, amount);
  }

  /**
   * Formats a monetary amount into a more text-friendly version.
   * @param amount The amount of currency to format.
   * @return The formatted amount.
   */
  @Override
  public String format(BigDecimal amount) {
    return eco.currency().format(amount.doubleValue());
  }

  /**
   * Formats a monetary amount into a more text-friendly version.
   * @param amount The amount of currency to format.
   * @param world The {@link World} in which this format operation is occurring.
   * @return The formatted amount.
   */
  @Override
  public String format(BigDecimal amount, String world) {
    return format(amount);
  }

  /**
   * Formats a monetary amount into a more text-friendly version.
   * @param amount The amount of currency to format.
   * @param world The {@link World} in which this format operation is occurring.
   * @param currency The {@link Currency} associated with the balance.
   * @return The formatted amount.
   */
  @Override
  public String format(BigDecimal amount, String world, String currency) {
    return format(amount);
  }

  public EconomyResponse setHoldings(Account account, BigDecimal amount) {
    if (!account.exists()) return AccountResponse.DOESNT_EXIST;

    TransactionResult result = account.setBalance(amount.doubleValue());

    switch (result) {
      case SUCCESS:
        return GeneralResponse.SUCCESS;
      case INSUFFICIENT_SPACE:
        return new CustomResponse(false, Language.LANG.plugin_vault_insufficientSpace);
      default:
        return GeneralResponse.FAILED;
    }
  }

  public EconomyResponse takeHoldings(Account account, BigDecimal amount) {
    if (!account.exists()) return AccountResponse.DOESNT_EXIST;

    TransactionResult result = account.remove(amount.doubleValue());

    switch (result) {
      case SUCCESS:
        return GeneralResponse.SUCCESS;
      case INSUFFICIENT_FUNDS:
        return HoldingsResponse.INSUFFICIENT;
      default:
        return GeneralResponse.FAILED;
    }
  }

  public EconomyResponse addHoldings(Account account, BigDecimal amount) {
    if (!account.exists()) return AccountResponse.DOESNT_EXIST;

    TransactionResult result = account.remove(amount.doubleValue());

    switch (result) {
      case SUCCESS:
        return GeneralResponse.SUCCESS;
      case INSUFFICIENT_SPACE:
        return new CustomResponse(false, Language.LANG.plugin_vault_insufficientSpace);
      default:
        return GeneralResponse.FAILED;
    }
  }

  /**
   * Purges the database of accounts with the default balance.
   * @return True if the purge was completed successfully.
   */
  @Override
  public boolean purgeAccounts() {
    return false;
  }

  /**
   * Purges the database of accounts with a balance under the specified one.
   * @param amount The amount that an account's balance has to be under in order to be removed.
   * @return True if the purge was completed successfully.
   */
  @Override
  public boolean purgeAccountsUnder(BigDecimal amount) {
    return false;
  }
}