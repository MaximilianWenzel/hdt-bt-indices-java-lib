package btindexmodels.facetedsearchmodels;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Objects;

public class AvailableFacet implements Comparable<AvailableFacet> {
    public UnifiedSet<Long> subjects;
    public String predicate;
    public UnifiedSet<Long> objects;

    @Override
    public int compareTo(AvailableFacet o) {

        if (subjects == null) {

            int comparison = Long.compare(objects.size(), o.objects.size());
            if (comparison == 0) {
                return predicate.compareTo(o.predicate);
            }
            return comparison;

        } else {

            int comparison = Long.compare(subjects.size(), o.subjects.size());
            if (comparison == 0) {
                return predicate.compareTo(o.predicate);
            }
            return comparison;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvailableFacet that = (AvailableFacet) o;
        return Objects.equals(predicate, that.predicate) &&
                Objects.equals(objects, that.objects) &&
                Objects.equals(subjects, that.subjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, objects);
    }
}
