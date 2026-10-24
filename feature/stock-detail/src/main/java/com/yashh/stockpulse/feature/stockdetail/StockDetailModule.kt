package com.yashh.stockpulse.feature.stockdetail

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class StockDetailModule {
    @Binds abstract fun bindRepository(impl: StockDetailRepositoryImpl): StockDetailRepository
}
