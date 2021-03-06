import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.*;
import java.awt.image.AffineTransformOp;
import static java.awt.event.MouseEvent.*;
import static java.awt.event.KeyEvent.*;
import javax.swing.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.*;
import java.net.*;
import java.util.*;
import java.util.Collections;
import com.mathworks.toolbox.javabuilder.* ;
import Panorama.* ;
import java.lang.*;

 
/**
 * <b>Map est la classe qui permet de créer et d'afficher le champ géoréférencé.</b>
 * <p>
 * Elle permet d'effectuer plusieurs actions:
 * <ul>
 * <li>Créer un panorama en faisant appel au fontion Matlab</li>
 * <li>Ajouter des marqueurs</li>
 * <li>Définir des régions</li>
 * <li>Enregistrer les images et recharger votre travail</li>
 * </ul>
 * </p>
 * <p>
 * De plus, des options de zoom et de centrage sont disponibles.
 * </p>
 * 
 * @see Map
 * 
 * @author benoit Franquet Corentin Floch
 * @version 1.0
 */
public class Map extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener{
        /**
         * Paramètre qui permet d'acceder à la position de souris
         * 
         */
	
	int x, y;
	int dx, dy;
	
	int startX;
	int startY;
	
	//indication de la position de l'image :
        /**
         * Parametre qui permet de définir la position de l'image
         */
	int offsetX;
	int offsetY;
	
	int offsetX_old;
	int offsetY_old;
	
	int endX;
	int endY;

	 /**
         * Paramètre permettant d'acceuillir l'image
         */
	BufferedImage image;
	BufferedImage imageInit;
	double scale;
	
	/**
        * Paramètre de dimension
        */
	int initWidth;
	int initHeight;
	
	int dimX;
	int dimY;
	
	/**
        * Paramètre permettant de détecter un drag de souris
        */
	boolean enableDrag;
	
	/**
        * Une map contient un panneau de recherche afin de lui envoyer les coordonnnées lors de clic droit
        */
	Recherche search;

        /**
         * Un menu apparait quand on clic sur la Map. Il permet d'accéder à diverses actions.
         */
	MouseMapMenu popupmenu;
	
        /**
         * Les Marqueurs et les lignes reliant ces marqueurs sont stockés sous la forme d'arraylist
         */
	public ArrayList<Pin> listPin;
	boolean removePin;
	int pinNumber;
	
	public ArrayList<StraightLine> listLine;
	boolean removeLine;
	public ArrayList<Point> listPoint;

	//copie des arraylist pour la fonction annuler
        /**
         * A chaque action, on enregistre l'état précédent de la Map dans une arraylist pour le restaurer
         */
	private ArrayList<Action> History;
	
        /**
         * Même donnée latitude et longitude mais converties en double pour en faciliter l'accès
         */
	//Coordonnée convertie
	double[][] Longitude;
	double[][] Latitude;

	//string à parser
	String lat,lon;

	//booleen pour savoir si on est dans le mode de capture
	boolean drawArea, areaDrawn;
	
	//Menu pour enregistrer la zone 
	CropMenu cropMenu;
	/**
	* Origine de la zone de drag&drop
	*/
	Point originCrop;
	
	/**
	* Le mode actuel de l'application
	*		
        */
	String mode;
               
        /**
        *	Les fichiers de coordonnés
        */        
        String nomLat;
        String nomLon;

	/**
        * constructeur Map
        * 
        * @param r
        *            Le panneau recherch
	* @param s
	*	     L'adresse de l'image
        * 
        * @see Map
        */
	Map(Recherche r,String s){
		mode = "Visualisation";
		drawArea = false;
		areaDrawn = false;
		
		Latitude = null;
		Longitude = null;
                
                search = new Recherche();
		search = r;
                lat = r.getlat;
                lon = r.getlon;

                setBackground(Color.black);
                
                addMouseListener(this);
                addMouseMotionListener(this);
		addMouseWheelListener(this);
				
		scale = 1.0;
		offsetX = 0;
                offsetY = 0;
                
                ////////////////////////////
		listPin = new ArrayList<Pin>();
		listPoint = new ArrayList<Point>();
		listLine = new ArrayList<StraightLine>();
		History = new ArrayList<Action>();		
		////////////////////////////
                loadImage(s);
		imageInit = image;
		
                Graphics2D g2 = image.createGraphics();
                g2.drawImage(image, offsetX, offsetY, this);
                g2.dispose();
                //ajout du menu propre au composant
                popupmenu = new MouseMapMenu(this);
                cropMenu = new CropMenu(this);
                originCrop = new Point();
        }


        /**
        * Peindre les différents composants dans le JPanel
        * Permet l'affichage des Pins, des Lignes et des notifications tooltips
	*
        * @param g
        *           Un graphics à peindre
        * 
        */
        public void paintComponent(Graphics g){               
		//dimensions de la zone graphique
                dimX = getWidth();
                dimY = getHeight();
		super.paintComponent(g);
                Graphics2D g2D = (Graphics2D) g;	
 		
		//dessin de l'image 			
                g2D.drawImage(image, offsetX, offsetY, this);         	
		//Choix de la couleur et de l'épaisseur des lignes         	
         	g2D.setColor(Color.red);
		g2D.setStroke(new BasicStroke(2));
		//Dessin des lignes		
		for(StraightLine s : listLine){
			g2D.drawLine(s.p1.getX() + 16, s.p1.getY() + 32, s.p2.getX() + 16, s.p2.getY() + 32);
			
		}
		g2D.setColor(Color.white);
		String coord = "";	
		//Dessin des pins			
		for(Pin p : listPin){
			p.draw(g2D);
			coord += p.getLatitude() + " N " + p.getLongitude() + " E";			
			g2D.drawString(coord,p.poffset.getX()+35,p.poffset.getY());
			coord = "";	
		}
		//Choix de la couleur de fond		
		g2D.setColor(Color.black);

		//Dessin d'un rectangle pour la capture de zone		
		//les deux tests sont nécessaires
		if(drawArea == true && areaDrawn == true){
			
			//gérer les cas négatifs
			if(x-startX >= 0 && y - startY >= 0){
				
				originCrop.setX(startX);
				originCrop.setY(startY);
			}
			else if(x-startX <= 0 && y - startY >= 0){
			
				originCrop.setX(x);
				originCrop.setY(startY);
			}
			
			else if(x-startX >= 0 && y - startY <= 0){
				
				originCrop.setX(startX);
				originCrop.setY(y);
			}
			
			else{
				originCrop.setX(x);
				originCrop.setY(y);
			}
			
			g2D.setStroke(new BasicStroke(1));
			g2D.drawRect(originCrop.getX(), originCrop.getY(), Math.abs(x-startX), Math.abs(y-startY));
		}

	    	g2D.dispose();   
        }

        		//////////////////////////////////////////////////////
        				//MouseListener//
			//////////////////////////////////////////////////////

        /**
         * Permet d'ajouter/supprimer un marqueur ou une ligne, ainsi que d'afficher un menu
         * 
         * @param e
         *            Un évènement e
         * 
         */
        //propre à Map et indépendant du popupmenu
        public void mouseClicked(MouseEvent e){
        	//la fonction est appelée lors d'un clic (appui + relache) (molette incluse)
		if(mode == "Panorama"){
			if(drawArea == false){
				if(e.getClickCount() == 1 && e.getButton() == BUTTON1){
					System.out.println("sélection");
				
					for(Pin p : listPin){
						if(e.getX() >= p.poffset.getX() && e.getX() <= p.pin.getWidth() + p.poffset.getX() & e.getY() >= p.poffset.getY() && e.getY() <= p.pin.getHeight() + p.poffset.getY()){
						//si on a cliqué sur une pin	
							if(listPoint.isEmpty() == false){
							//si la liste de points n'est pas vide
								if(listPoint.get(0).getX() == p.poffset.getX() + p.pin.getWidth()/2 && listPoint.get(0).getY() == p.poffset.getX() + p.pin.getHeight()){
									//on retire le point s'il y est déjà
									listPoint.remove(0);
								}
							
								else{	//sinon on l'ajoute : c'est le 2e
									listPoint.add(p.poffset);
									listPoint.get(1).setX(p.poffset.getX());
									listPoint.get(1).setY(p.poffset.getY());
									
									//regarder ici s'il n'y a pas une ligne à supprimer
									// c'est à dire si la ligne était déjà tracée
									removeLine = false;
									History.add(new Action(listPin,listLine));
									for(StraightLine s : listLine){
									
										if(s.matchWith(listPoint.get(0), listPoint.get(1))){
	
											listLine.remove(listLine.indexOf(s));
											removeLine = true;
											break;
										}
									}
									//dans le cas contraire :
									if(removeLine == false){
	
										History.add(new Action(listPin,listLine));
										listLine.add(new StraightLine(listPoint.get(0), listPoint.get(1)));
									}
								
									//on supprime les points :
									listPoint.remove(1);
									listPoint.remove(0);
									
									repaint();
								}
							}
						
							else{	//la liste est vide on ajoute le premier point
								listPoint.add(p.poffset);
								listPoint.get(0).setX(p.poffset.getX());
								listPoint.get(0).setY(p.poffset.getY());
							}
						}
					}		
				}
			
				if(e.getClickCount() == 2 && e.getButton() == BUTTON1){
				
					System.out.println("double clique gauche");
				
					//Si on est dans l'image :
					if(e.getX() >= offsetX && e.getX() <= image.getWidth() + offsetX && e.getY() >= offsetY && e.getY() <= image.getHeight() + offsetY)
					{
						//Pour obtenir la position réelle du curseur en enlevant l'effet du zoom et de l'offset
						int px =(int)((e.getX()-offsetX)/scale);
						int py =(int)((e.getY()-offsetY)/scale);
						double Lt = Latitude[py][px];
						double Ln = Longitude[py][px];			
				
						if(Lt != 0 && Ln != 0){
							History.add(new Action(listPin,listLine));
							pinMap(e.getX(), e.getY());
							//on marque ou non la carte avec des épingles
						}
					}
				}
									
				else{
					//Pour obtenir la position réelle du curseur en enlevant l'effet du zoom et de l'offset
					int px =(int)((e.getX()-offsetX)/scale);
					int py =(int)((e.getY()-offsetY)/scale);
	
 					//attention ligne puis colonne, j'espère que c'est ça
					if ( Latitude != null ){				
						if (px >= 0  && py >= 0 && px < initWidth && py < initHeight){
							//on accède aux bons champs
							double Lt = Latitude[py][px];
							double Ln = Longitude[py][px];
							//on envoit les coordonnées dans le panneau de recherche						
					 		search.setCoord(Lt,Ln);	
						}
					}
					else
					{
						search.setCoord(py,px);
					}
				}
			}
		}
		
		if(e.getButton() == BUTTON3){
                		
                	//l'instance de popupmenu est crée dans le constructeur
			popupmenu.show(this, e.getX(), e.getY());
            		popupmenu.setVisible(true);		
		}		
        }
        /////////////////////////// FIN DE LA ZONE MATLAB ///////////////////////        
                
	public void mouseEntered(MouseEvent e){        
        }
                
        public void mouseExited(MouseEvent e){        
        }

        /**
         * Permet de détecter le début dun drag
         * 
         * @param e
         *            un évènement de souris
         * 
         */                
        public void mousePressed(MouseEvent e){
        	//la fonction est appelée lors de l'appui sur un bouton (molette incluse)
        	startX = e.getX();
                startY = e.getY(); 
                
                      	if(e.getButton() == BUTTON1){
                		enableDrag = true;
                		drawArea = false;
                		//récupération de la position de l'image
                		offsetX_old = offsetX;
                		offsetY_old = offsetY;
                	
                		//récupération de la position des pin
				for(Pin p : listPin){
              		
					p.poffset_old.setX(p.poffset.getX());
					p.poffset_old.setY(p.poffset.getY());
				}
				repaint();
                	}
                	//si on appui sur le bouton droit on dessine la zone
                	else if(e.getButton() == BUTTON3){
       				System.out.println("Sélection de zone activée.");
       				enableDrag = false;
       				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
       				//en début de pression le rectangle n'est pas dessiné
       				areaDrawn = false; 
       				drawArea = true;
       				repaint();
       			}
                
                	else{
                		//arret du drag si appui sur un autre bouton
                		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                		enableDrag = false;
                		drawArea = false;     
                		repaint();                     	
                	}  		
        }

        /**
        * Détecter si un bouton de souris est relaché.
        * 
        * @param e
        * 
        */        
	public void mouseReleased(MouseEvent e){
                //La fonction est appelée lorsqu'un bouton est relaché (molette incluse)
                endX = e.getX();
                endY = e.getY();
                        
                System.out.println("relache en ("+endX+";"+endY+")");
                
                if(e.getButton() == BUTTON1 && drawArea == false){
                	
                	setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
              		System.out.println("Position de l'image : ("+offsetX+";"+offsetY+")"); 
               		enableDrag = false;
               	}
               	
               	if(drawArea == true && areaDrawn == true ){
                	setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        		
                	cropMenu.show(this, e.getX(), e.getY());
            		cropMenu.setVisible(true);	
            		repaint();	
            		//drawArea = false;	//ne pas le mettre ici 
                }
                
                if(drawArea == true && areaDrawn == false){
                //il n'y a pas eu de mouseDragged
                	drawArea = false;
                	repaint();
                }
               	           	
	}

        		//////////////////////////////////////////////////////
        				//MouseMotionListener//
			//////////////////////////////////////////////////////
	/**
        * Permet de bouger l'image en modifiant son offset lorque l'on drag
        * 
        * @param e
        *           Un évènement de souris.
        * 
        */		
	public void mouseDragged(MouseEvent e){
		//la fonction est appelée lorsqu'il y a mouvement alors qu'un bouton est enfoncé (molette incluse)
                //il faut donc limiter le fonctionnement voulu au bouton gauche de la souris
                
                if(enableDrag == true){
               		setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
               		//position de la souris lorsque appuyé et déplacé
                	x = e.getX();
			y = e.getY();
                	repaint();
                       
                	dx = e.getX() - startX;
			dy = e.getY() - startY;
			
			offsetX = offsetX_old + dx;
			offsetY = offsetY_old + dy;
			
			for(Pin p : listPin){
              		
				p.poffset.setX(p.poffset_old.getX() + dx);
				p.poffset.setY(p.poffset_old.getY() + dy);
			}
		}
		
		if(drawArea){
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			x = e.getX();
			y = e.getY();
			areaDrawn = true;
			repaint();
		}
        }

        /**
         * Afficher une notification tooltip contenant Latitude et Longitude
         * 
         * @param e
         *            un évènement clavier
         * 
         */
	public void mouseMoved(MouseEvent e){
	
		///////////////////////// Debut de la zone Matlab mcr requis /////////////////////////
		// ATTENTION CERTAINES FONCTIONS SONT CONÇUES POUR POUVOIR FONCTIONNER SANS COMME LE //
		// TOOLTIP ET L'ECRITURE DANS LE PANEL DE RECHERCHE QUI UTILISERONT ALORS LES COORDS //
		//Pour obtenir la position réelle du curseur en enlevant l'effet du zoom et de l'offset
		int px =(int)((e.getX()-offsetX)/scale);
		int py =(int)((e.getY()-offsetY)/scale);
		String coord = "";

		//si la matrice de coordonnées est non vide
		if ( Latitude != null ){
			//on regarde si le pixel est bien dans l'image
			if ( px >=0 && py >= 0 && px < initWidth && py < initHeight){
				try {
					float Lt = (float)Latitude[py][px];
					float Ln = (float)Longitude[py][px];
					if(Lt != 0 && Ln != 0){	
						coord = "Lat: " + Lt + " Lon: " + Ln ;
						//on affiche une bulle d'information
						setToolTipText(coord);	
					}
				} catch (IndexOutOfBoundsException ei){
					ei.printStackTrace();
	 			}	
			}		
		}
		else
		{				
			//cette partie permet de faire fonctionner la fonction
			//si le panorama n'est pas créer
			coord = "Lat: " + px + " Lon: " + py ;
			//on affiche une bulle d'information
			setToolTipText(coord);	
		}	
        }
	
			//////////////////////////////////////////////////////
        				//MouseWheelListener//
			//////////////////////////////////////////////////////
	
        /**
         * Modifier le facteur de zoom avec la molette de souris
         * 
         * @param e
         *            un évènement de souris.
         * 
         */
	public void mouseWheelMoved(MouseWheelEvent e){
	
		//la fonction est appelée lorsque la molette est tournée
		//vers le haut : e.getWheelRotation = -1
		//vers le bas : e.getWheelRotation = +1
		//Si on est dans l'image :
		if(e.getX() >= offsetX && e.getX() <= image.getWidth() + offsetX && e.getY() >= offsetY && e.getY() <= image.getHeight() + offsetY)
		{
		
			if(e.getWheelRotation() == -1 && scale < 10){
				scale = scale + 0.1;
			}
			else if(e.getWheelRotation() == 1){
				scale = scale - 0.1;
				
				if(scale < 0.1){
					scale = 0.1;
				}
			}
		
			System.out.println("Echelle : "+scale);
			System.out.println("Zone à zoomer : ("+e.getX()+";"+e.getY()+")");
			setScale(e.getX(), e.getY());
		}	
	}
	
        /**
         * Permet d'appliquer la transformation affine liée au scale
         * 
         * @param a
         *            la coordonnée en x de la souris
	 * @param b 
	 *	      la coordonnée en y de la souris
         * 
         */
	public void setScale(int a, int b)  
    	{  
		double w = image.getWidth();
		
        	AffineTransform tx = new AffineTransform();
        	tx.scale(scale, scale);
       		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
       		BufferedImage biNew = new BufferedImage((int)(initWidth*scale), (int) (initHeight*scale), imageInit.getType());
    		op.filter(imageInit, biNew);
		image = biNew;
    	

		//image est une nouvelle instance indépendante de imageInit
		
		//si on zoom sur l'image :
		double factor = initWidth*scale/w;
		
		//Mise à jours des offsets		
		offsetX -= (int)((a-offsetX)*factor - a + offsetX);
		offsetY -= (int)((b-offsetY)*factor - b + offsetY);
		
		//mise à jour des positions des pins :
		for(Pin p : listPin){
			p.poffset.setX(p.poffset.getX() - (int)((a-p.poffset.getX())*factor - a + p.poffset.getX()));
			p.poffset.setY(p.poffset.getY() - (int)((b-p.poffset.getY())*factor - b + p.poffset.getY()));
		}
     				
		repaint();
    	}
    	
        /**
         * Remmetre l'image en (0,0)
         * 
         */
    	public void initLocation(){
    		image = imageInit;
    		//mettre à jour les pins
    		for(Pin p : listPin){
			p.poffset.setX((int)((p.poffset.getX()-offsetX)/scale));
			p.poffset.setY((int)((p.poffset.getY()-offsetY)/scale));		
		}
    		
    		offsetX = 0;
    		offsetY = 0;
    		
    		repaint();
    	}

        /**
         * Fonction appelée pour charger l'image par défaut
         * 
         */
	 public void loadImage()  
    	 {  
        	String fileName = "resources/Images/ensea.jpg";
  		//modifier le bloc try catch : on ne se sert pas d'URL
       		try  
        	{  
        		URL defaultImage = Map.class.getResource(fileName);
            		File photo = new File(defaultImage.toURI());
        		image =ImageIO.read(photo);
        		imageInit = image;
        		
        		initWidth = image.getWidth();
			initHeight = image.getHeight();
			scale = 1;
        		//imageInit référence la meme instance que l'image chargée 
        	}	  
        	catch(MalformedURLException mue)  
        	{
            		System.out.println("URL trouble: " + mue.getMessage());
        	}
        	catch(IOException ioe)
        	{  
            		System.out.println("read trouble: " + ioe.getMessage());
        	} 
        	catch(Exception ae){
        		ae.printStackTrace();
        	} 
    	} 


        /**
        * Fonction appelée pour charger une image spécifique
        * 
        * @param s
        *            Le chemin de l'image.
        * 
        */
	public void loadImage(String s)  
    	{  
        	String fileName = s; 
		//modifier le bloc try catch : on ne se sert pas d'URL
       		try{  
			File photo = new File(fileName);
        		image =ImageIO.read(photo);
        		imageInit = image;
        		offsetX = 0;
        		offsetY = 0;
        		listPin.clear();
        		listLine.clear();
        		History.clear();
        		initWidth = image.getWidth();
			initHeight = image.getHeight();
			scale = 1;
        		//imageInit référence la meme instance que l'image chargée 
        	}	  
        	catch(MalformedURLException mue)  
        	{  
            		System.out.println("URL trouble: " + mue.getMessage());  
        	}  
        	catch(IOException ioe)  
        	{  
            		System.out.println("read trouble: " + ioe.getMessage());  
        	}  
    	}
	
	/**
	* Permet de charger une image lors de la création d'un itineraire avant le vol
	*
	* @param b
	*	 L'image renvoyée par google lors de la création de l'itinéraire
	*
	*/
	public void loadImage(BufferedImage b){
		image = b;
		imageInit = image;
		initWidth = image.getWidth();
		initHeight = image.getHeight();
		scale = 1;
		offsetX = 0;
		offsetY = 0;
		repaint();
	}
	/**
        * Permet de sauvergarder les données gps fournis en sortie de Matlab
        * 
        * @param lat
        *            La matrice de latitude convertie en double.
	* @param lon
	*	     La matrice de longitude convertie en double
	* @param LAT
	*	     Le nom du fichier de latitude
	* @param LON
	*	     Le nom du fichier de longitude 
        * 
        */
	public void RecordCoord(double[][] lat, double[][] lon,String LAT, String LON){
		//cette méthode permet d'enregistrer les données pour ne plus àvoir a les recharger		
		Latitude = lat;
		Longitude = lon;
		nomLat = LAT;
		nomLon = LON;
	}
	
        /**
         * Permet de rechercher la position d'un pixel et de centrer l'image en un point donné
         * 
         * @param lat
         *            La latitude du point recherché
	 * @param lon
	 *	      La longitude du point recherché
         * 
         */
	public void searchResult(double lat, double lon){
		//Centrer l'image au pixel indiqué
		try {			
			//on otient les tailles des tableaux
			//System.out.println(Latitude.length+"\n"+Latitude[0].length);
			int l = Latitude.length;
			int c = Latitude[0].length;
			int px = 0;
			int py = 0;
			boolean isInPicture = false;
			
			for(int i = 0;i<l;i++){
				for(int j=0;j<c;j++){
					if(Latitude[i][j] == lat && Longitude[i][j] == lon){
						px = j;
						py = i;
						isInPicture = true;
						//on sort
						i = l;
						j = c;
					}
				}
			}
			//on centre la carte sur le pixel si les coords renseignés sont dans les tableaux
			if(isInPicture == true){
				for(Pin p: listPin){
					p.poffset.setX((int)((p.poffset.getX()-offsetX)/scale));
					p.poffset.setY((int)((p.poffset.getY()-offsetY)/scale));
				}
				
				int Px = (int) Math.floor(px*scale+offsetX);
				int Py = (int) Math.floor(offsetY+py*scale);	
				//décalle ie on centre la zone
				offsetX = dimX/2 - (Px - offsetX);
				offsetY = dimY/2 - (Py - offsetY);
				//mise à jours de pins
				for(Pin p: listPin){
					p.poffset.setX(p.poffset.getX()+offsetX);
					p.poffset.setY(p.poffset.getY()+offsetY);
				}
				repaint();	
			}			
		} catch (Exception ei){
			ei.printStackTrace();		
		}	
	}	

        /**
         * Permet de placer les pins sur la carte
         * 
         * @param x
         *            La coordonnée en x de la souris.
	 * @param y
	 *	      La coordonnée en y de la souris
         * 
         */	
	public void pinMap(int x, int y){
	//il y a eu double clic ici pas nécessairement
		if(x >= offsetX && x <= image.getWidth() + offsetX
		&& y >= offsetY && y <= image.getHeight() + offsetY){
			if(listPin.isEmpty() == false){
				//si le liste n'est pas vide :
				if(listPoint.isEmpty() == false){
					listPoint.remove(0);
				}
				//si la liste contient quelque chose, on peut enlever un élément :
				removePin = false;
						
				for(Pin p : listPin){	
					if(x >= p.poffset.getX() && x <= p.pin.getWidth() + p.poffset.getX()
					&& y >= p.poffset.getY() && y <= p.pin.getHeight() + p.poffset.getY()){
						
						pinNumber = listPin.indexOf(p);
						removePin = true;
						//supprimer un élément de la liste modifie sa taille
						//ne pas supprimer la pin ici
						//il n'y a qu'une seule ocurrence de pin quoique....zoom
					}
				}
						
				if(removePin == true){
					removeLine = true;
					while(removeLine == true){
						removeLine = removeElement();
						//on supprime toutes les liaisons
					}

					//on supprime la Pin
					listPin.remove(pinNumber);
				}
					
				else{
					Pin pin = pinSetCoord(x,y);		
					listPin.add(pin);
					listPoint.add(listPin.get(listPin.size()-1).poffset);
					listPoint.add(listPin.get(listPin.size()-2).poffset);

					listLine.add(new StraightLine(listPoint.get(0), listPoint.get(1)));
					
					listPoint.remove(1);
					listPoint.remove(0);
				}
			}
					
			else{	//ici la liste de Pin est vide
				//cette fonction créer la Pin et lui attribut les bonnes valeurs de coordonnées
				Pin pin = pinSetCoord(x,y);		
				listPin.add(pin);
			}
			
			repaint();
		}	
	}

        /**
        * Fonction qui va réglé les coordonnées gps d'un point avant de l'ajouter à la liste de pin
        * 
        * @param x
        *            La coordonnée en x d'un point
	* @param y
	* 	     La coordonnée ne y d'un point
        * 
	* @return    Le pin a ajouté à la liste de pin
        */
	public Pin pinSetCoord(int x,int y){
		//Cette méthode permet d'associer les bons coordonnées au pin
		double Lt;
		double Ln;
		Pin pin = new Pin(x,y,this);
		//on revient au vrai coordonnées				
		int px =(int)((pin.poffset.getX()+16-offsetX)/scale);
		int py =(int)((pin.poffset.getY()+32-offsetY)/scale);
				

		if ( Latitude != null ){
			if ( px >=0 && py >= 0 && px < initWidth && py < initHeight){
				try {
					Lt = Latitude[py][px];
					Ln = Longitude[py][px];	
					pin.setCoord(Lt,Ln);
				} catch (IndexOutOfBoundsException ei){
					ei.printStackTrace();
				}	
			}		
			else
			{	
				System.out.println(px+" "+py);				
				Lt = (double)px;
				Ln = (double)py;
				pin.setCoord(Lt,Ln);
			}
		}
		else
		{	
			System.out.println(px+" "+py);			
			Lt = (double)px;
			Ln = (double)py;
			pin.setCoord(Lt,Ln);
		}
		//on renvoit le pin modifié
		return pin;	
	}	

	/**
        * Permet de supprimer les lignes attachées à une pin
        * 
        * @return un booléen montrant l'état de la liste de ligne 
        */
	public boolean removeElement(){
	
		//History.add(new Action(listPin,listLine));
		for(StraightLine s : listLine){		
			if(s.own(listPin.get(pinNumber).poffset)){
				listLine.remove(listLine.indexOf(s));
				return true;
			}
		}
		return false;
	}

        /**
        * Permet de sauvergarder les pins et les lignes qui lui sont relié dans un fichier texte réutilisable
        * 
        */
	public void savePin(){
		JFileChooser chooser = new JFileChooser();
   		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);	
		chooser.setAcceptAllFileFilterUsed(false);
 		int returnVal = chooser.showOpenDialog(this.getParent());    			
    		if(returnVal == JFileChooser.APPROVE_OPTION){
			String map = chooser.getSelectedFile().getAbsolutePath()+"/mosaique.png";
			String pin = chooser.getSelectedFile().getAbsolutePath()+"/positions.txt";
			String latf = chooser.getSelectedFile().getAbsolutePath()+"/latitude.txt";
			String lonf = chooser.getSelectedFile().getAbsolutePath()+"/longitude.txt";
   	 		try{
				FileOutputStream fos = new FileOutputStream(map);
				//on enregistre bien le buffered image pour ne pas prendre en compte les effets de zooom
				ImageIO.write(image,"jpg",fos);
			} catch(IOException e){
				e.printStackTrace();
			}
			if(listPin.size() != 0){
				toString(pin);
			}
			//on déplace le fichier gps
			deplacer(new File(nomLat),new File(latf));
			deplacer(new File(nomLon),new File(lonf));
		} 
	}

        /**
        * Permet de sauvergarder la zone visible du Graphic2D, les pins ne seront plus modifiables
        * 
        */
	public void saveImage() {
		//fonction de sauvegarde de l'image elle sera appelée dans Fenetre via le menu
   		int w = this.getWidth();
   		int h = this.getHeight();
   		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
   		Graphics2D g = bi.createGraphics();
   		paint(g);
		JFileChooser chooser = new JFileChooser();
   		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);	
		chooser.setAcceptAllFileFilterUsed(false);
 		int returnVal = chooser.showOpenDialog(this.getParent());    			
    		if(returnVal == JFileChooser.APPROVE_OPTION){
			String map = chooser.getSelectedFile().getAbsolutePath()+"/travail.jpg";
   	 		try{
				FileOutputStream fos = new FileOutputStream(map);
				ImageIO.write(bi,"jpg",fos);
			} catch(IOException e){
				e.printStackTrace();
			}
		} 
	}

	public void startDraw(){
		drawArea = true;
	}

        /**
        * Permet de découper une zone de l'image encadrant au mieux les pins
        * 
        */
        public void cropMap(){
        	int w = this.getWidth();
   		int h = this.getHeight();
   		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
   		Graphics2D g = bi.createGraphics();
   		paint(g);
   		
		CropImageFilter cif = new CropImageFilter(originCrop.getX(), originCrop.getY(), Math.abs(x-startX), Math.abs(y-startY));
		
		Image im1 = createImage(new FilteredImageSource(bi.getSource(), cif));
		
		BufferedImage bufImage = new BufferedImage(im1.getWidth(null), im1.getHeight(null), BufferedImage.TYPE_INT_RGB);
		bufImage.getGraphics().drawImage(im1, 0, 0, null);
		JFileChooser chooser = new JFileChooser();
    		FileNameExtensionFilter filter = new FileNameExtensionFilter("JPG Images", "jpg");
    		chooser.setFileFilter(filter);
   			
   		int returnVal = chooser.showOpenDialog(this.getParent());
    		
    		if(returnVal == JFileChooser.APPROVE_OPTION)
    		{
			//on enregistre
	   	 	try{
				String fichier = chooser.getSelectedFile().getAbsolutePath();
				System.out.println(fichier);
        			FileOutputStream fos = new FileOutputStream(fichier);
				ImageIO.write(bufImage,"jpg",fos);
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
		
		drawArea = false;
		repaint();
        }
        /**
        * Permet d'enregister l'état de la Map 
        * 
        * @param args
        *            Le chemin ou sera sauver les pins et la map.
        * 
        * @return une chaine représentant le composant
	*
        */
	public String toString(String args){
		try{		
			File monFichier = new File(args);
			BufferedWriter bw = new BufferedWriter(new FileWriter(monFichier)) ;
			int taille = listPin.size();
			int i = 0;			
			for ( i = 0; i < taille; i++){
				String ligne = ""; 
				int xp = listPin.get(i).poffset.getX();
				int yp = listPin.get(i).poffset.getY();
				
				//retour au vrai coordonnées
				xp = (int)((xp-offsetX)/scale);
				yp = (int)((yp-offsetY)/scale);

				double latp = listPin.get(i).getLatitude();
				double lonp = listPin.get(i).getLongitude();

				ligne = "Pixel "+xp+" "+yp+" Coord "+latp + " " +lonp;
				bw.write(ligne,0,ligne.length()-1);
				bw.newLine();
				bw.flush();
			}
			//on enregistre les lignes
			for (StraightLine s: listLine){
				String l = "";
				int p1 = -1;
				int p2 = -1;
				int k = listLine.indexOf(s);
				for(Pin p : listPin){
					if (s.own(p.poffset)){
						if(p1 == -1 && p2 == -1){
							p1 = listPin.indexOf(p);
						}
						else if(p1 != -1 && p2 == -1){
							p2 = listPin.indexOf(p);
						}				
					}
				}
				System.out.println(p2+" "+p1);
				//on enregistre 
				if(p1 != -1 && p2 != -1){
					l = "Ligne " + p1+" "+p2+" l";
					bw.write(l,0,l.length()-1);
					bw.newLine();
					bw.flush();
				}
			}
		}
		catch(IOException ex){
                       ex.printStackTrace();
		}
		return "Fichier enregistre";
	}

        /**
        * Fonction qui permet de reconstruire la map, en lisant dans un fichier précédement ceci afin
	* de restaurer l'état des pins et des lignes
        * 
        * @param fichier
        *            L'emplacement du fichier texte
        * 
        */
	public void readWork(String fichier){
		//on créer un buffer pour lire le fichier
		try{
			scale = 1/scale;
			File monFichier = new File(fichier);
			FileReader fichierlu = new FileReader(monFichier);
			BufferedReader bufferlu = new BufferedReader(fichierlu);

			listPin.clear();
			listLine.clear();

			String ligne = null;
			String[] resultat = null;
			int xp,yp,n1,n2;
			double latp,lonp;		
		
			//temps que l'on lit une ligne
			while ((ligne = bufferlu.readLine()) != null) {	
				//Parser les infos et créer les nouveaux points
				resultat = ligne.split(" ");//on récupère les différents champs
				//convertir les strings en entier/double		
				switch(resultat[0]){				
					case "Pixel" :	xp = Integer.parseInt(resultat[1])+32;
							yp = Integer.parseInt(resultat[2])+16;
							latp = Double.parseDouble(resultat[4]);
							lonp = Double.parseDouble(resultat[5]);
							Pin p = pinSetCoord(xp,yp);
							p.setCoord(latp,lonp);			
							listPin.add(p);
							break;
					case "Ligne" :	//les pins sont enregistrés avant les lignes donc c'est ok
							n1 = Integer.parseInt(resultat[1]);
							n2 = Integer.parseInt(resultat[2]);

							listPoint.add(listPin.get(n1).poffset);
							listPoint.add(listPin.get(n2).poffset);
							listLine.add(new StraightLine(listPoint.get(0), listPoint.get(1)));
							listPoint.remove(1);
							listPoint.remove(0); 
							break;
					default : break;
				}
			}
			//on ferme le buffer
			bufferlu.close();
			
			repaint();
		} catch(Exception e){
			e.printStackTrace();
		}	
	}

        /**
        * permet d'annuler la dernière action
        * 
        */	
	public void cancelOne(){
		//annule le dernier ajout/suppression de pin ou ligne
		//on peut revenir autant de fois que l'on veut en arrière très facilement
		if(History.size() != 0){
			if(History.get(History.size()-1).getListPin().size() != listPin.size()){
				//ceci permet de restaurer le pin et les lignes qui lui étaient attachées
				listPin = new ArrayList<Pin>(History.get(History.size()-1).getListPin());
				listLine = new ArrayList<StraightLine>(History.get(History.size()-1).getListLine());
			}
			else if(History.get(History.size()-1).getListLine().size() != listLine.size()){
				listPin = new ArrayList<Pin>(History.get(History.size()-1).getListPin());
				listLine = new ArrayList<StraightLine>(History.get(History.size()-1).getListLine());	
			}
			//L'action a été supprimée on l'enlève de l'historique
			History.remove(History.size()-1);
			repaint();
		}
	}	

        /**
         * Permet de restaurer l'état initiale de la map via le menu Édition, ie supprimer pin et ligne
         * 
         */
	public void cancelAll(){
		if(scale != 0){
			scale = 1;
		}
		setScale(0,0);
		offsetX = 0;
		offsetY = 0;
		listPin.clear();
		listLine.clear();
		History.clear();
		repaint();
	}
	
	/**
	*	Fonction pour déplacer un fichier
	*	@param source
	* 		Le fichier qui va etre déplacer
	*	@param destination
	*		Le Fichier ou sera déplacer le fichier
	*/
	public void deplacer(File source,File destination) {
        	if( !destination.exists() ) {
        	        // On supprime si le fichier existe déjà
        	        boolean suppr = destination.delete();
        	}
        	boolean result = source.renameTo(destination);
	}
	/**
	*	Fonction pour régler le mode
	*	@param m
	*		Le mode de l'application 
	*/
	public void setMode(String m){
		String oldmode = mode;
		if (oldmode == "Panorama" && m == "Visualisation"){
			Latitude = null;
			Longitude = null;
		}
		mode = m;
		popupmenu.enabledMenu();
	}
	/**
	*	On obtient le mode actuel de l'application
	*	@return le mode
	*
	*/
	public String getMode(){
		return mode;
	}
	/**
	*	accéder à la liste de pin
	*	@return la liste de pin
	*/
	public ArrayList<Pin> getListPin(){
		return listPin;	
	}
	/**
	*	acceder à l'offsetX
	*	@return l'offsetX
	*/
	public int getOffsetX(){
		return offsetX;
	}
	/**
	*	acceder à l'offsetY
	*	@return l'offsetY
	*/
	public int getOffsetY(){
		return offsetY;
	}
	
	/**
	*	acceder au scale
	*	@return Le scale
	*/
	public double getScale(){
		return scale;
	}
}



