package dev.nohus.rift.industry.cookbook

import dev.nohus.rift.network.Result
import org.koin.core.annotation.Factory

@Factory
class ResolveBlueprintAndGetBuildCostUseCase(
    private val blueprintNameResolver: BlueprintNameResolver,
    private val eveCookbookApi: EveCookbookApi,
) {

    sealed interface InputResolution {
        data class Resolved(val typeId: Int, val displayName: String) : InputResolution
        data class Ambiguous(val candidates: List<BlueprintNameResolver.Match>) : InputResolution
        data object NotFound : InputResolution
    }

    data class BuildCostQueryResult(
        val resolution: InputResolution,
        val responses: List<EveCookbookApi.BuildCostResponse> = emptyList(),
    )

    /**
     * Minimal end-to-end example:
     * user input (can be Chinese alias) -> typeId -> /api/buildCost request
     */
    suspend operator fun invoke(
        userInput: String,
        quantity: Int = 1,
    ): Result<BuildCostQueryResult> {
        return when (val resolution = blueprintNameResolver.resolve(userInput)) {
            is BlueprintNameResolver.ResolveResult.NotFound -> {
                Result.Success(BuildCostQueryResult(InputResolution.NotFound))
            }

            is BlueprintNameResolver.ResolveResult.Ambiguous -> {
                Result.Success(
                    BuildCostQueryResult(
                        resolution = InputResolution.Ambiguous(resolution.matches),
                    ),
                )
            }

            is BlueprintNameResolver.ResolveResult.Resolved -> {
                val request = EveCookbookApi.BuildCostRequest(
                    blueprintTypeIds = listOf(resolution.match.typeId),
                    quantity = quantity,
                )
                eveCookbookApi.getBuildCost(request).map { responses ->
                    BuildCostQueryResult(
                        resolution = InputResolution.Resolved(
                            typeId = resolution.match.typeId,
                            displayName = resolution.match.enName,
                        ),
                        responses = responses,
                    )
                }
            }
        }
    }
}
