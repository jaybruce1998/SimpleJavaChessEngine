public class SearchWindow {
    public int Floor;//Alpha
    public int Ceiling;//Beta

    public static SearchWindow infinite() {
        return new SearchWindow(Short.MIN_VALUE, Short.MAX_VALUE);
    }

    public SearchWindow UpperBound()
    {
        return new SearchWindow(Ceiling - 1, Ceiling);
    }
    public SearchWindow LowerBound()
    {
        return new SearchWindow(Floor, Floor + 1);
    }
    //used to quickly determine that a move is not improving the score for color.
    public SearchWindow GetLowerBound(Color color)
    {
        return color == Color.White ? LowerBound() : UpperBound();
    }
    //used to quickly determine that a move is too good and will not be allowed by the opponent.
    public SearchWindow GetUpperBound(Color color)
    {
        return color == Color.White ? UpperBound() : LowerBound();
    }

    public SearchWindow(SearchWindow sw) {
        Floor = sw.Floor;
        Ceiling = sw.Ceiling;
    }

    public SearchWindow(int floor, int ceiling) {
        Floor = floor;
        Ceiling = ceiling;
    }

    public boolean Cut(int score, Color color) {
        if (color == Color.White) //Cut floor
        {
            if (score <= Floor)
                return false; //outside search window

            Floor = score;
        } else {
            if (score >= Ceiling) //Cut ceiling
                return false; //outside search window

            Ceiling = score;
        }
        return Floor >= Ceiling; //Cutoff?
    }

    public boolean FailLow(int score, Color color) {
        return color == Color.White ? (score <= Floor) : (score >= Ceiling);
    }

    public boolean FailHigh(int score, Color color) {
        return color == Color.White ? (score >= Ceiling) : (score <= Floor);
    }

    public int GetScore(Color color) {
        return color == Color.White ? Floor : Ceiling;
    }

    public boolean CanFailHigh(Color color) {
        return color == Color.White ? (Ceiling < Short.MAX_VALUE) : (Floor > Short.MIN_VALUE);
    }
}