package valenota;

import robocode.*;
import java.awt.Color;

/**
 * HoneyMouth - a sample robot by ValeNota team.
 * 
 * This robot scans for enemy robots, tracks and targets a specific enemy,
 * adjusts its gun and movement to optimize.
 *
 * @author Paulo Edney da Silva Junior (original)
 * @author Luan Santiago de Araujo (contributor)
 * @author Jean Victor Oliveira de Melo (contributor)
 * @author Lucas Sales Moreira Cordeiro (contributor)
 */

public class HoneyMouth extends AdvancedRobot {

    private static final double DISTANCIA_TIRO_PROXIMO = 100;
    private static final double MAX_TIRO = 3;
    private static final double MIN_TIRO = 0.1;
    private static final int ANGULO_ESQUIVA = 45;
    private static final double VELOCIDADE_MOVIMENTO = 5;
    private static final double LIMIAR_DANO = 30;
    private static final double LIMIAR_PERDA_ENERGIA = 0.1;
    private static final double LIMITE_DISTANCIA_LATERAL = 100;
    private static final double LIMIAR_PERTENCIMENTO_DISTANCIA_LATERAL = 50;
    private static final double ALTA_DISTANCIA_LATERAL = 1.0;
    private static final double BAIXA_DISTANCIA_LATERAL = 0.0;

    private boolean focarInimigo = false;
    private int contadorDisparoProximo = 0;
    private double ultimaEnergiaInimigo = 100.0;
    private String alvoAtual = null;
    private double ultimoRumoInimigo = 0.0;
    private double ultimaDistanciaInimigo = 0.0;
    private int contagemDanoConsecutivo = 0;
    private int contagemPerdaEnergiaConsecutiva = 0;

    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setColors(Color.YELLOW, Color.BLACK, Color.RED);

        while (true) {
            turnRadarRight(Double.POSITIVE_INFINITY);
        }
    }

    public void onScannedRobot(ScannedRobotEvent event) {
        if (alvoAtual == null) {
            alvoAtual = event.getName();
        } else if (!event.getName().equals(alvoAtual)) {
            return; // Ignora outros robôs se estiver focado em um alvo específico
        }

        double angulo = getHeading() + event.getBearing() - getGunHeading();
        setTurnGunRight(angulo);

        if (getGunHeat() == 0) {
            if (event.getDistance() < DISTANCIA_TIRO_PROXIMO && getEnergy() > 30) {
                focarInimigo = true;
                contadorDisparoProximo = 0;
                double tiro = calcularForcaTiro(event.getEnergy());
                setFire(tiro);
            } else {
                focarInimigo = false;
                contadorDisparoProximo++;
                setFire(1);
            }
        }

        setTurnRadarRight(getHeading() + event.getBearing() - getRadarHeading());
        setTurnRight(event.getBearing() + 90 - (15 * contadorDisparoProximo));
        setAhead(VELOCIDADE_MOVIMENTO);

        double energiaInimigo = event.getEnergy();
        if (energiaInimigo != ultimaEnergiaInimigo) {
            contagemDanoConsecutivo++;
            if (contagemDanoConsecutivo >= LIMIAR_DANO || contagemPerdaEnergiaConsecutiva >= LIMIAR_PERDA_ENERGIA) {
                moverParaLongeDoInimigo(ultimoRumoInimigo, ultimaDistanciaInimigo);
                contagemDanoConsecutivo = 0; // Reinicia os contadores
                contagemPerdaEnergiaConsecutiva = 0;
            }
            ultimaEnergiaInimigo = energiaInimigo;
        } else {
            contagemDanoConsecutivo = 0; // Reinicia o contador se não receber dano
            contagemPerdaEnergiaConsecutiva++; // Incrementa o contador de perda de energia
        }

        ultimoRumoInimigo = event.getBearing();
        ultimaDistanciaInimigo = event.getDistance();
    }

    public void onHitByBullet(HitByBulletEvent event) {
        setTurnRight(ANGULO_ESQUIVA);
        setAhead(VELOCIDADE_MOVIMENTO);
    }

    public void onHitWall(HitWallEvent event) {
        setTurnRight(180);
        setAhead(VELOCIDADE_MOVIMENTO);
    }

    public void onBulletHit(BulletHitEvent event) {
        if (focarInimigo) {
            contadorDisparoProximo = 0;
        }
    }

    public void onBulletMissed(BulletMissedEvent event) {
        if (focarInimigo) {
            contadorDisparoProximo++;
        }
    }

    public void onRobotDeath(RobotDeathEvent event) {
        if (event.getName().equals(alvoAtual)) {
            alvoAtual = null; // Reinicia o alvo atual quando ele for eliminado
        }
    }

    public void onRoundEnded(RoundEndedEvent event) {
        alvoAtual = null; // Reinicia o alvo atual no final da rodada
    }

    private void moverParaLongeDoInimigo(double rumoInimigo, double distanciaInimigo) {
        double x = getX();
        double y = getY();
        double larguraCampo = getBattleFieldWidth();
        double alturaCampo = getBattleFieldHeight();
        double distanciaLateral = Math.min(x, Math.min(y, Math.min(larguraCampo - x, alturaCampo - y)));

        // Lógica fuzzy para a distância lateral
        double pertencimentoDistanciaLateral = ALTA_DISTANCIA_LATERAL;
        if (distanciaLateral <= LIMIAR_PERTENCIMENTO_DISTANCIA_LATERAL) {
            pertencimentoDistanciaLateral = BAIXA_DISTANCIA_LATERAL;
        }

        // Lógica fuzzy para o ângulo de movimento
        double anguloMovimento = rumoInimigo + 180;
        if (pertencimentoDistanciaLateral == BAIXA_DISTANCIA_LATERAL) {
            anguloMovimento = rumoInimigo + 90;
        }

        // Lógica fuzzy para a distância de movimento
        double distanciaMovimento = distanciaInimigo + 100;
        if (pertencimentoDistanciaLateral == BAIXA_DISTANCIA_LATERAL) {
            distanciaMovimento = distanciaInimigo + 200;
        }

        setTurnRight(anguloMovimento);
        setAhead(distanciaMovimento);
    }

    private double calcularForcaTiro(double energiaInimigo) {
        double forcaTiro = MAX_TIRO;

        if (energiaInimigo > getEnergy()) {
            forcaTiro = Math.max(MIN_TIRO, Math.min(MAX_TIRO, energiaInimigo / 4));
        }

        return forcaTiro;
    }
}