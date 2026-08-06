package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.checkCast
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal const val SAM_QUERY_PARAMS = "Lcom/afreecatv/data/model/SAMQueryParams;"

internal fun samAdResponseFilters() = listOf(
    methodCall(
        parameters = listOf(SAM_QUERY_PARAMS, "Ljava/util/Map;", "L"),
        returnType = "Ljava/lang/Object;",
    ),
    checkCast("L"),
    opcode(Opcode.RETURN_OBJECT, MatchAfterImmediately()),
)