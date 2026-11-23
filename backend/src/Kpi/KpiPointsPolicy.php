<?php
declare(strict_types=1);

namespace WebSitePlus\Kpi;

/** Approved shared points scale for a valid KPI evaluation. */
final class KpiPointsPolicy
{
    public static function forPercentage(float $percentage): int
    {
        if (!is_finite($percentage) || $percentage < 0) {
            throw new \InvalidArgumentException('Invalid KPI percentage');
        }
        return match (true) {
            $percentage >= 90 => 20,
            $percentage >= 80 => 17,
            $percentage >= 60 => 14,
            $percentage >= 40 => 12,
            default => 10,
        };
    }
}
