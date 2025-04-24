public class SortedMove extends Move implements Comparable<SortedMove>
{
    public final float Priority;

    public SortedMove(Move move, float Priority)
    {
        super(move.FromSquare, move.ToSquare, move.Promotion);
        this.Priority=Priority;
    }

    public int compareTo(SortedMove other)
    {
        return Float.compare(other.Priority, Priority);
    }
}