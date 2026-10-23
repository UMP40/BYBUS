#pragma once

#include <stddef.h>
#include <stdint.h>
#include <string.h>

// Pure planning for the two constant JP=2 language getters. A verified value
// getter is at least two instructions long, so an exactly 8-byte constant
// return fits without any trampoline; a leading BTI needs 12 bytes. The host
// fixture exercises this same planner and apply function.
namespace limbus_language {

enum class Strategy { kReject, kDirect };

struct Plan {
    Strategy strategy = Strategy::kReject;
    size_t width = 0;
    // At most one preserved BTI landing pad plus movz/ret.
    uint32_t words[3] = {};
};

inline bool is_bti_landing_pad(uint32_t word) {
    return word == 0xd503241f ||  // bti
           word == 0xd503245f ||  // bti c
           word == 0xd503249f ||  // bti j
           word == 0xd50324df;    // bti jc
}

// `next_method` is the next entry in the complete managed-method scan. The
// direct patch never writes past this boundary, so an unknown or too-short
// boundary is rejected instead of falling back to a 16/20-byte trampoline.
inline Plan plan_language_getter_patch(uintptr_t target, uintptr_t next_method,
                                       uint32_t first_word) {
    Plan plan;
    if (target == 0 || (target & 3u) != 0 || next_method <= target) return plan;
    const size_t distance = static_cast<size_t>(next_method - target);
    const bool bti = is_bti_landing_pad(first_word);
    const size_t width = bti ? 12u : 8u;
    if (distance < width) return plan;
    plan.strategy = Strategy::kDirect;
    plan.width = width;
    size_t index = 0;
    if (bti) plan.words[index++] = first_word;
    plan.words[index++] = 0x52800040u;  // movz w0, #2 (LOCALIZE_LANGUAGE.JP)
    plan.words[index] = 0xd65f03c0u;    // ret
    return plan;
}

// Writes exactly the planned bytes. The caller owns page protection and cache
// maintenance; the host fixture calls this same function on its own buffer.
inline void apply_direct_plan(void *target, const Plan &plan) {
    if (target != nullptr && plan.strategy == Strategy::kDirect && plan.width != 0)
        memcpy(target, plan.words, plan.width);
}

}  // namespace limbus_language
