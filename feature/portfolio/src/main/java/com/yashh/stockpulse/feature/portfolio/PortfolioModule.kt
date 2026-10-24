package com.yashh.stockpulse.feature.portfolio

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PortfolioModule {
    @Binds abstract fun bindRepository(impl: PortfolioRepositoryImpl): PortfolioRepository
}
