package giavu.co.jp.taxifare.di

import giavu.co.jp.domain.usecase.FetchNearestSupportCityUseCase
import giavu.co.jp.taxifare.map.FetchMyLocationUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.Module

/**
 * @Author: Hoang Vu
 * @Date:   2019-08-21
 */
class UseCaseModule {
    val module: Module = org.koin.dsl.module.module {
        single { FetchNearestSupportCityUseCase(api = get()) }
        factory { FetchMyLocationUseCase(context = androidApplication()) }
    }
}