package evolutionsim;

import java.awt.*;
import java.util.ArrayList;

public class Adult extends Puck {

    double cooldown;
    ArrayList<Egg> children;
    ArrayList<Adult> mateable;
    double age;
    Adult mate;

    Adult(double[] dnaValues, double xcor, double ycor, double mass, int generation) {
        super(dnaValues, xcor, ycor, mass, generation);
        translate();
        cooldown = 0;
        age = 0;
        children = new ArrayList<Egg>();
        mateable = new ArrayList<Adult>();
        Adult mate = null;
    }

    public void updateVars() {
        if (Sim.pause) {
            return;
        }
        if (cooldown <= 0) {
            v += power.pheno * Sim.ticklength / mass;
            determineMateable();
            pursue();
            seek();
            mass -= (power.pheno / 2000000);
            cooldown = freq.pheno;
        }
        cooldown -= Sim.ticklength;
        if (mass <= 0) {
            die();
        } else {
            mass -= Sim.ticklength * (mass / 100);
        }
        age += Sim.ticklength / 60;
        updateRadius();
    }

    public void go() {
        updateVars();
        motion();
        mateCollision();
        melonCollision();
    }

    public void motion() {
        v = Math.max(0, v + ((-0.1 * v * v) + friction.pheno * Sim.ticklength) / mass);
        xv = Math.cos(heading) * v;
        yv = Math.sin(heading) * v;
        x = x + xv * (Sim.ticklength);
        y = y + yv * (Sim.ticklength);
        if ((int) x < 0 || (int) y < 0 || (int) x > Sim.canvasWidth || (int) y > Sim.canvasHeight) {
            heading += Math.PI;
        }
    }

    public void determineMateable() {
        if (children.size() > 0) {
            return;
        }
        for (Adult a : Sim.adultList) {
            if (a.children.size() == 0 && !a.equals(this)) {
                if (mateScore(a) > standards.pheno) {
                    mateable.add(a);
                }
            }
        }
    }

    public void pursue() {
        for (Adult a : mateable) {
            if (a.mateable.contains(this) && Sim.average(childCount.pheno, a.childCount.pheno) > 0) {
                mate = a;
                a.mate = this;
                mateable = new ArrayList<Adult>();
                a.mateable = new ArrayList<Adult>();
            }
        }
    }

    public void mate() {
        heading = getHeadingTowards(mate);
    }

    private double mateScore(Adult potMate) {
        return (1 / (((1.75) * children.size() * children.size()) + 1)) *
                (mass + potMate.mass + potMate.mass / getDistanceTo(potMate));
        // return (mass + (potMate.mass * potMate.mass)/getDistanceTo(potMate));
    }

    public void seek() {
        if (mate != null) {
            mate();
        } else if (children.size() > 0) {
            seekFood();
        } else {
            seekFood();
        }
    }

    public void seekFood() {
        if (Sim.melonList.size() == 0) {
            return;
        }
        if (see() != null) {
            towardsVision();
        } else if (smell() != null) {
            towardsSmell();
        }
    }

    public Melon see() {
        if (Sim.pause) {
            return null;
        }
        if (Sim.melonList.size() == 0) {
            return null;
        }
        double target, potential;
        Melon t = Sim.melonList.get(0);
        for (Melon m : Sim.melonList) {
            target = melonScore(t);
            potential = melonScore(m);
            if (potential > target) {
                t = m;
            }
        }
        if (getDistanceTo(t) > vision.pheno) {
            return null;
        }
        mass -= getDistanceTo(t) * (1 / 10000);
        return t;
    }

    public void towardsVision() {
        Melon m = see();
        if (m != null) {
            heading = getHeadingTowards(m);
        }
    }

    public double melonScore(Melon m) {
        return (Math.pow(m.mass, melSizePref.pheno) / (getDistanceTo(m) * getDistanceTo(m)));
    }

    public double[] smell() {
        if (Sim.pause) {
            return null;
        }
        double xcor, ycor, strength, newX, newY, newStrength;
        xcor = ycor = newX = newY = strength = newStrength = 0.0;
        for (Melon m : Sim.melonList) {
            newStrength = melonScore(m);
            strength += newStrength;
            newX = (m.x - x) * newStrength;
            newY = (m.y - y) * newStrength;
            xcor += newX;
            ycor += newY;
        }
        // [xcor, ycor, dist, heading]
        double[] output = new double[] { xcor, ycor, Math.hypot(xcor, ycor), Math.atan2(ycor, xcor) };
        if (output[0] > smell.pheno) {
            return null;
        }
        mass -= output[2] * (1 / 20000);
        return output;
    }

    public void towardsSmell() {
        if (smell() != null) {
            heading = smell()[3];
        }
    }

    public void mateCollision() {
        if (Sim.adultList.size() < 2) {
            return;
        }
        for (Adult a : Sim.adultList) {
            if (getDistanceTo(a) <= radius + a.radius && mate == a) {
                Sim.layEggs(this, a);
                mate = null;
                a.mate = null;
            }
        }
    }

    public boolean melonCollision() {
        if (Sim.melonList.size() == 0) {
            return false;
        }
        double massPer;
        for (Melon m : Sim.melonList) {
            if (getDistanceTo(m) <= radius + m.radius) {
                massPer = m.mass / children.size();
                for (Egg c : children) {
                    c.mass += massPer;
                }
                mass += m.mass;
                m.die();
                return true;
            }
        }
        return false;
    }

    public void debug(Graphics2D g) {
        if (see() != null) {
            g.setStroke(new BasicStroke(2));
            g.setColor(Color.GREEN);
            g.drawLine((int) x, (int) y, (int) (x + 5 * Math.cos(getHeadingTowards(see())) * melonScore(see())),
                    (int) (y + 5 * Math.sin(getHeadingTowards(see())) * melonScore(see())));
            g.setStroke(new BasicStroke(1));
            g.setColor(Color.CYAN);
            g.drawLine((int) x, (int) y, (int) see().x, (int) see().y);
        } else if (smell() != null) {
            g.setColor(Color.MAGENTA);
            g.drawLine((int)x, (int)y, (int)(x + 20 * Math.cos(smell()[3])), (int)(y + 20 * Math.sin(smell()[3])));
            //g.drawLine((int) x, (int) y, (int) (x + 15 * smell()[0]), (int) (y + 15 * smell()[1]));
        }
        g.setColor(Color.GREEN);
        for (int i = 0; i < children.size(); i++) {
            Egg c = children.get(i);
            g.drawLine((int)x, (int)y, (int)c.x, (int)c.y);
        }
    }

    public void draw(Graphics2D g) {
        drawX = x - radius;
        drawY = y - radius;
        Color tail = new Color((int) (power.geno * 0.256), (int) (freq.geno * 0.256), (int) (friction.geno * 0.256));
        Color eye = new Color((int) ((vision.geno + smell.geno) * 0.128), (int) (melSizePref.geno * 0.256), (int) (mutationChance.geno * 0.256));
        Color body = new Color((int) (standards.geno * 0.256), (int) (childCount.geno * 0.256),
                (int) (maturation.geno * 0.256));
        g.setColor(tail);
        /**
        int[] xDraw = { (int) ((x) + Math.cos(heading) * radius * (3 / 4)),
                (int) ((x) - Math.cos(heading + Math.PI / 6)
                        * (radius + radius * (1.5 * power.geno / 1000) + radius * (cooldown / freq.pheno + 0.5))),
                (int) ((x) - Math.cos(heading - Math.PI / 6)
                        * (radius + radius * (1.5 * power.geno / 1000) + radius * (cooldown / freq.pheno + 0.5))) };
        int[] yDraw = { (int) ((y) + Math.sin(heading) * radius * (3 / 4)),
                (int) ((y) - Math.sin(heading + Math.PI / 6)
                        * (radius + radius * (1.5 * power.geno / 1000) + radius * (cooldown / freq.pheno + 0.5))),
                (int) ((y) - Math.sin(heading - Math.PI / 6)
                        * (radius + radius * (1.5 * power.geno / 1000) + radius * (cooldown / freq.pheno + 0.5))) };
        **/
        int[] xDraw = { (int) ((x) + Math.cos(heading) * radius * (3 / 4)),
                (int) ((x) - Math.cos(heading + Math.PI / 6)
                        * (radius + radius * (cooldown / (freq.pheno + 0.1)))),
                (int) ((x) - Math.cos(heading - Math.PI / 6)
                        * (radius + radius * (cooldown / (freq.pheno + 0.1)))) };
        int[] yDraw = { (int) ((y) + Math.sin(heading) * radius * (3 / 4)),
                (int) ((y) - Math.sin(heading + Math.PI / 6)
                        * (radius + radius * (cooldown / (freq.pheno + 0.1)))),
                (int) ((y) - Math.sin(heading - Math.PI / 6)
                        * (radius + radius * (cooldown / (freq.pheno + 0.1)))) };
        g.fillPolygon(xDraw, yDraw, 3);
        g.setColor(body);
        g.fillOval((int) drawX, (int) drawY, (int) radius * 2, (int) radius * 2);
        g.setColor(Color.WHITE);
        g.fillOval((int) (x - radius / 2), (int) (y - radius / 2), (int) (radius), (int) (radius));
        g.setColor(eye);
        g.fillOval((int) (x - 0.5 * (radius * (vision.geno + smell.geno) / 2000)),
                (int) (y - 0.5 * (radius * (vision.geno + smell.geno) / 2000)),
                (int) (radius * (vision.geno + smell.geno) / 2000), (int) (radius * (vision.geno + smell.geno) / 2000));
        g.setColor(Color.BLACK);
        g.fillOval((int) (x - 0.5 * (radius * (vision.geno) / 2000)), (int) (y - 0.5 * (radius * (vision.geno) / 2000)),
                (int) (radius * (vision.geno) / 2000), (int) (radius * (vision.geno) / 2000));
    }
}
