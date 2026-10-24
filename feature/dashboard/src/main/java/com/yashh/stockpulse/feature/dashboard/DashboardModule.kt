package com.yashh.stockpulse.feature.dashboard

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {
    @Binds abstract fun bindRepository(impl: DashboardRepositoryImpl): DashboardRepository
}
