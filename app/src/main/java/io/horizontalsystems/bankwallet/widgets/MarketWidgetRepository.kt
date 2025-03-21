package io.horizontalsystems.bankwallet.widgets

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesMenuService
import io.horizontalsystems.bankwallet.modules.market.favorites.WatchlistSorting
import io.horizontalsystems.bankwallet.modules.market.favorites.period
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformsRepository
import kotlinx.coroutines.rx2.await

class MarketWidgetRepository(
    private val marketKit: MarketKitWrapper,
    private val favoritesManager: MarketFavoritesManager,
    private val favoritesMenuService: MarketFavoritesMenuService,
    private val topPlatformsRepository: TopPlatformsRepository,
    private val currencyManager: CurrencyManager
) {
    companion object {
        private const val topGainers = 100
        private const val itemsLimit = 5
    }

    private val currency
        get() = currencyManager.baseCurrency

    suspend fun getMarketItems(marketWidgetType: MarketWidgetType): List<MarketWidgetItem> =
        when (marketWidgetType) {
            MarketWidgetType.Watchlist -> {
                getWatchlist(
                    favoritesMenuService.listSorting,
                    favoritesMenuService.manualSortOrder,
                    favoritesMenuService.timeDuration
                )
            }

            MarketWidgetType.TopGainers -> {
                getTopGainers()
            }

            MarketWidgetType.TopPlatforms -> {
                getTopPlatforms()
            }
        }

    private suspend fun getTopPlatforms(): List<MarketWidgetItem> {
        val platformItems = topPlatformsRepository.get(
            sortingField = SortingField.HighestCap,
            timeDuration = TimeDuration.OneDay,
            currencyCode = currency.code,
            forceRefresh = true,
            limit = itemsLimit
        )
        return platformItems.map { item ->
            MarketWidgetItem(
                uid = item.platform.uid,
                title = item.platform.name,
                subtitle = Translator.getString(
                    R.string.MarketTopPlatforms_Protocols,
                    item.protocols
                ),
                label = item.rank.toString(),
                value = App.numberFormatter.formatFiatShort(
                    item.marketCap,
                    currency.symbol,
                    2
                ),
                diff = item.changeDiff,
                blockchainTypeUid = null,
                imageRemoteUrl = item.platform.iconUrl
            )
        }
    }

    private suspend fun getTopGainers(): List<MarketWidgetItem> {
        val marketItems = marketKit.marketInfosSingle(topGainers, currency.code, false)
            .await()
            .map { MarketItem.createFromCoinMarket(it, currency) }

        val sortedMarketItems = marketItems
            .subList(0, Integer.min(marketItems.size, topGainers))
            .sort(SortingField.TopGainers)
            .subList(0, Integer.min(marketItems.size, itemsLimit))

        return sortedMarketItems.map { marketWidgetItem(it) }
    }

    private suspend fun getWatchlist(
        listSorting: WatchlistSorting,
        manualSortOrder: List<String>,
        timeDuration: TimeDuration
    ): List<MarketWidgetItem> {
        val favoriteCoins = favoritesManager.getAll()
        var marketItems = listOf<MarketItem>()

        if (favoriteCoins.isNotEmpty()) {
            val favoriteCoinUids = favoriteCoins.map { it.coinUid }
            marketItems = marketKit.marketInfosSingle(favoriteCoinUids, currency.code)
                .await()
                .map { marketInfo ->
                    MarketItem.createFromCoinMarket(marketInfo, currency, timeDuration.period)
                }

            if (listSorting == WatchlistSorting.Manual) {
                marketItems = marketItems.sortedBy {
                    manualSortOrder.indexOf(it.fullCoin.coin.uid)
                }
            } else {
                val sortField = when (listSorting) {
                    WatchlistSorting.HighestCap -> SortingField.HighestCap
                    WatchlistSorting.LowestCap -> SortingField.LowestCap
                    WatchlistSorting.Gainers -> SortingField.TopGainers
                    WatchlistSorting.Losers -> SortingField.TopLosers
                    else -> throw IllegalStateException("Manual sorting should be handled separately")
                }
                marketItems = marketItems.sort(sortField)
            }
        }

        return marketItems.map { marketWidgetItem(it) }
    }

    private fun marketWidgetItem(
        marketItem: MarketItem,
    ): MarketWidgetItem {

        return MarketWidgetItem(
            uid = marketItem.fullCoin.coin.uid,
            title = marketItem.fullCoin.coin.code,
            subtitle = marketItem.marketCap.getFormattedShort(),
            label = marketItem.fullCoin.coin.marketCapRank?.toString() ?: "",
            value = App.numberFormatter.formatFiatFull(
                marketItem.rate.value,
                marketItem.rate.currency.symbol
            ),
            diff = marketItem.diff,
            blockchainTypeUid = null,
            imageRemoteUrl = marketItem.fullCoin.coin.imageUrl,
            alternativeRemoteUrl = marketItem.fullCoin.coin.alternativeImageUrl
        )
    }

}
